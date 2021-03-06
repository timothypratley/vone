(ns vone.version-one-request-tests
  (:require [vone.version-one-request :refer :all]
            [midje.sweet :refer :all]))


(facts
 (fact (unmap [:a :b] [{:a 1, :b 2}
                       {:c 3, :b 4}])
       => [[1 2]
           [nil 4]])
 (fact (clean-xml "some&#x19;text") => "sometext")
 (fact (clean-xml "some&#x2fA;text") => "sometext"))
