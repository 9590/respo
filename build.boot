
(set-env!
  :asset-paths #{"assets/"}
  :resource-paths #{"polyfill" "src"}
  :dependencies '[[org.clojure/clojure         "1.8.0"       :scope "provided"]
                  [org.clojure/clojurescript   "1.9.473"     :scope "provided"]
                  [adzerk/boot-cljs            "1.7.228-1"   :scope "provided"]
                  [adzerk/boot-reload          "0.4.13"      :scope "provided"]
                  [binaryage/devtools          "0.9.1"       :scope "provided"]
                  [cirru/boot-stack-server     "0.1.30"      :scope "provided"]
                  [andare                      "0.3.0"       :scope "provided"]
                  [cumulo/shallow-diff         "0.1.3"       :scope "provided"]
                  [mvc-works/hsl               "0.1.2"       :scope "provided"]
                  [mvc-works/polyfill          "0.1.1"]])

(require '[adzerk.boot-cljs   :refer [cljs]]
         '[adzerk.boot-reload :refer [reload]])

(def +version+ "0.3.39")

(task-options!
  pom {:project     'respo/respo
       :version     +version+
       :description "A front-end MVC library"
       :url         "https://github.com/Respo/respo"
       :scm         {:url "https://github.com/Respo/respo"}
       :license     {"MIT" "http://opensource.org/licenses/mit-license.php"}})

(deftask dev []
  (comp
    (watch)
    (reload :on-jsload 'respo.main/on-jsload
            :cljs-asset-path ".")
    (cljs :compiler-options {:language-in :ecmascript5})
    (target :no-clean true)))

(deftask build-advanced []
  (comp
    (cljs :optimizations :advanced
          :compiler-options {:language-in :ecmascript5
                             :pseudo-names true
                             :parallel-build true
                             :optimize-constants true
                             :source-map true})
    (target :no-clean true)))

; some problems due to uglifying
(deftask build []
  (comp
    (pom)
    (jar)
    (install)
    (target)))

(deftask deploy []
  (set-env! :repositories #(conj % ["clojars" {:url "https://clojars.org/repo/"}]))
  (comp
    (build)
    (push :repo "clojars" :gpg-sign (not (.endsWith +version+ "-SNAPSHOT")))))
