(ns vone.parse-tests
  (:require [noir.session :as session])
  (:use [vone.helpers]
        [vone.version-one-request]
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
                 m (xml-collapse x 0)]
             (is (= m
                    [{"EndDate" (parse-date "2012-08-08")
                      "BeginDate" (parse-date "2012-07-18")}])))))


(comment
(deftest test-cumulative-on-status
  ;;(with-redefs [session/get {:username "" :password ""}]
               ;;(xhr (cumulative-on-status-query "TC+Sharks" "TC1211"
                                                  ;;(parse-date "2012-08-08")
                                                  ;;"Accepted"))]
         (let [x
 "<?xml version='1.0' encoding='UTF-8'?>
 <History total='1' pageSize='2147 483647' pageStart='0'>
 <Asset href='/Tideworks/VersionOne/rest-1.v1/Data/Timebox/475626/1416294' id='Timebox:475626:1416294'>
 <Attribute name=\"Workitems:PrimaryWorkitem[Team.Name='TC Sharks';Status.Name='Accepted'].Estimate[AssetState!='Dead'].@Sum\">57</Attribute></Asset></History>"
              m (xml-collapse x 0)]
           (pprint (singular m))))
  )

(comment
(deftest test-empty-xml
    (let [x
"<?xml version='1.0' encoding='UTF-8'?>
<History total='1' pageSize='2147483647' pageStart='0'>
<Asset href='/Tideworks/VersionOne/rest-1.v1/Data/Timebox/475626/1335982' id='Timebox:475626:1335982'>
<Attribute name=\"Workitems:PrimaryWorkitem[Team.Name='TC Sharks';Status.Name='Shelved'].Estimate[AssetState!='Dead'].@Sum\" /></Asset></History>"
          m (xml-collapse x 0)]
      (pprint m)))
  )

;(deftest test-names
  ;(testing "Names can retrieve a list of statuses"
           ;(println (names "username" "password" "StoryStatus"))))




