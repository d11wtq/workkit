(ns cuckoo.cron.parse-test
  (:use clojure.test
        [cuckoo.cron.parse :only [parse]])
  (:import java.util.Date))

(deftest parse-test
  (testing "with a 7-component cron"
    (testing "with all wildcards"
      (testing "expands to complete ranges"
        (is (= {:year        (set (range 1970 2100))
                :month       (set (range 1 13))
                :day         (set (range 1 32))
                :day-of-week (set (range 0 7))
                :hour        (set (range 0 24))
                :minute      (set (range 0 60))
                :second      (set (range 0 60))}
               (parse "* * * * * * *"
                      (Date. (- 2014 1900) 0 1))))))

    (testing "with an exact second"
      (testing "expands to the single second"
        (is (= #{10}
               (:second (parse "10 * * * * * *"
                               (Date. (- 2014 1900) 0 1)))))))

    (testing "with a list of seconds"
      (testing "expands to the listed set"
        (is (= #{10 20 30}
               (:second (parse "10,20,30 * * * * * *"
                               (Date. (- 2014 1900) 0 1)))))))

    (testing "with a range of seconds"
      (testing "expands to the set in the range"
        (is (= #{15 16 17 18 19 20}
               (:second (parse "15-20 * * * * * *"
                               (Date. (- 2014 1900) 0 1)))))))

    (testing "with wildcard seconds in steps"
      (testing "expands to the set in the full range using the step"
        (is (= #{0 4 8 12 16 20 24 28 32 36 40 44 48 52 56}
               (:second (parse "*/4 * * * * * *"
                               (Date. (- 2014 1900) 0 1)))))))

    (testing "with a range of seconds in steps"
      (testing "expands to the set in the range using the step"
        (is (= #{0 10 20 30}
               (:second (parse "0-30/10 * * * * * *"
                               (Date. (- 2014 1900) 0 1)))))))

    (testing "with a mix of formats for seconds"
      (testing "expands to the unionized set"
        (is (= #{3 4 7 10 12 14 40}
               (:second (parse "3,4,7,10-15/2,40 * * * * * *"
                               (Date. (- 2014 1900) 0 1)))))))

    (testing "with an exact day"
      (testing "expands to the single day"
        (is (= #{3}
               (:day (parse "* * * * 3 * *"
                            (Date. (- 2014 1900) 0 1)))))))

    (testing "with a wildcard day in feb"
      (testing "expands to the set in the range 1-28"
        (is (= (set (range 1 29))
               (:day (parse "* * * * * * *"
                            (Date. (- 2014 1900) 1 1)))))))

    (testing "with a wildcard day in a leap year feb"
      (testing "expands to the set in the range 1-29"
        (is (= (set (range 1 30))
               (:day (parse "* * * * * * *"
                            (Date. (- 2016 1900) 1 1)))))))

    (testing "with a wildcard day in june"
      (testing "expands to the set in the range 1-30"
        (is (= (set (range 1 31))
               (:day (parse "* * * * * * *"
                            (Date. (- 2014 1900) 5 1)))))))

    (testing "with a wildcard day in steps"
      (testing "expands to the set in the range using step"
        (is (= #{1 5 9 13 17 21 25}
               (:day (parse "* * * * */4 * *"
                            (Date. (- 2014 1900) 1 1)))))

        (is (= #{1 5 9 13 17 21 25 29}
               (:day (parse "* * * * */4 * *"
                            (Date. (- 2016 1900) 1 1)))))))

    (testing "parsing a complex string"
      (is (= {:year        #{2014 2015 2016 2017}
              :month       #{1 2 3 4 5 6 7 8 9 10 11 12}
              :day         #{1 3 5 7 9 11 13 15 17 19 21 23 25 27 29 31}
              :day-of-week #{0 1 2 3 4 5 6}
              :hour        #{3 4 5 17 19}
              :minute      (set (range 0 60))
              :second      #{0 10 20 30 40 50}}
             (parse "*/10 * 3-5,17,19 * */2 * 2014-2017"
                    (Date. (- 2014 1900) 0 1)))))))