(ns vone.views.service
  (:use [vone.models.queries]
        [noir.core]
        [noir.response :only [redirect json]]))

;TODO: make submit go the right place first instead of redirecting
(defpage [:post "/burndown"] {:keys [team sprint]}
  (redirect (str "/burndown/" team "/" sprint)))

(defpage "/burndown/:team/:sprint" {:keys [team sprint]}
  (println "burndown " team sprint)
  (json (cons ["Day" "ToDo"]
              (for-sprint team sprint
                          todo-on))))

;TODO: make submit go the right place first instead of redirecting
(defpage [:post "/cumulative"] {:keys [team sprint]}
  (redirect (str "/cumulative/" team "/" sprint)))

(defpage "/cumulative/:team/:sprint" {:keys [team sprint]}
  (println "cumulative " team sprint)
  (let [statuses (names "StoryStatus")]
    (json (cons (cons "Day" statuses)
                (for-sprint team sprint
                            (partial cumulative-on statuses))))))
