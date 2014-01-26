(ns workkit.schedule
  "WorkKit distributed Clojure task scheduler."
  (:require [taoensso.carmine :as redis]
            [workkit.cron :as cron]
            [workkit.job :as job])
  (:import java.util.Date))

(defn create
  "Create a new WorkKit schedule instance with the given name.

  Opts currently only supports :redis which is a map of the form:

    {:uri \"redis://127.0.0.1:9376/\"}

  This is the default connection uri."
  ([name]
   (create name nil))
  ([name opts]
   {:name name
    :conn {:pool nil
           :spec (:redis opts)}}))

(defmacro with-schedule
  [schedule & body]
  `(redis/wcar (:conn ~schedule)
     ~@body))

(defn jobs-key
  "Returns the name of the redis key storing WorkKit job payloads."
  [schedule]
  (format "%s:jobs" (:name schedule)))

(defn next-key
  "Returns the name of the redis key storing WorkKit next run times."
  [schedule]
  (format "%s:next" (:name schedule)))

(defn insert-job
  "Inserts WorkKit the job payload data into the schedule.

  Returns false on failure, such as when the job already exists."
  [schedule cron-string job args]
  (= 1
     (with-schedule schedule
       (redis/hset
         (jobs-key schedule)
         (job/id cron-string job args)
         (job/payload cron-string job args)))))

(defn queue-next
  "Places a WorkKit job onto the run-queue for the time it should next run."
  [schedule cron-string job args]
  (with-schedule schedule
    (redis/zadd
      (next-key schedule)
      (clj-time.coerce/to-long (cron/next-time cron-string (Date.)))
      (job/id cron-string job args))))

(defn insert*
  "Inserts a job into a WorkKit schedule and returns the job ID.

  The macro workkit.schedule/insert expands to an application of insert*.

  The function to be applied must be quoted. Arguments to be passed to the
  function for application must be serializable to JSON."
  [schedule time-type time-text job & args]
  (let [cron-string (cron/build time-type time-text)
        job-id (job/id cron-string job args)]
    (when (insert-job schedule cron-string job args)
      (queue-next schedule cron-string job args))
    (job/id cron-string job args)))

(defn process
  "Process jobs from the schedule, blocking the current thread.

  While this function blocks, each job is processed in a new thread."
  ([schedule]
   (process schedule 1)
   (recur schedule))
  ([schedule max-jobs]
   (when (> max-jobs 0)
     (future (println "Processing..."))
     (recur schedule (dec max-jobs)))))