(ns vone.parse-tests
  (:use [vone.models.queries]
        [clojure.test]
        [clojure.pprint]
        [clojure.xml :as xml]))

(deftest test-parse
  (testing "Conversion of VersionOne xml into a clojure structure"
           (let [x "<Assets total='1' pageSize='2147483647' pageStart='0'>
                      <Asset href='/Tideworks/VersionOne/rest-1.v1/Data/Timebox/475626' id='Timebox:475626'>
	                      <Attribute name='BeginDate'>2012-07-18</Attribute>
	                      <Attribute name='EndDate'>2012-08-08</Attribute>
	                    </Asset>
	                  </Assets>"
                 s (java.io.ByteArrayInputStream. (.getBytes x "UTF-8"))
                 x (xml/parse s)]
             (is (= (collapse x)
                    [{:EndDate "2012-08-08", :BeginDate "2012-07-18"}])))))

;(deftest test-names
  ;(testing "Names can retrieve a list of statuses"
           ;(println (names "username" "password" "StoryStatus"))))
