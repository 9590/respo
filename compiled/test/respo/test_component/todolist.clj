
(ns respo.test-component.todolist
  (:require [respo.alias :refer [create-comp div]]
            [respo.test-component.task :refer [comp-task]]))

(defn render [tasks]
  (fn [state mutate]
    (div
      {}
      (->> tasks (map (fn [task] [(:id task) (comp-task task)]))))))

(def comp-todolist (create-comp :todolist render))
