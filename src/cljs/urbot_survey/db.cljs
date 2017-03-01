(ns urbot-survey.db
  (:require [loom.graph :as loom]))

(def default-db
  {:survey {:graph {:lookup {:a {:prompt "a prompt"
                                 :input {:type "yesno"
                                         :data {:labels {:yes "Yeah"
                                                         :no "Nah"}}}}
                             :b {:prompt "you said yes!"}
                             :c {:prompt "you said no!"}}
                    :loom (into {} (loom/weighted-digraph [:a :b 1] [:a :c 0]))}
            :artifacts [{:href "https://milieu.io/en/wakefield"
                         :background "rgb(100,100,100)"
                         :label "Lorne Shoudice Spring, La Pêche, Quebec, Canada"}]
            :active-node-id :a
            :responses {}}})
