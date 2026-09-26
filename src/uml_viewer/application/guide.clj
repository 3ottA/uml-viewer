(ns uml-viewer.application.guide
  "A built-in, authored tour of this repository's diagram workflow."
  (:require [clojure.java.io :as io]
            [clojure.string :as str]))

(def components
  {:main {:text "The entry points start diagram generation and the viewer. The generator and viewer run separately."
          :source {:ns "uml-viewer.main.ir-generator" :name "-main"}}
   :adapters {:text "The adapters turn viewer state into a Quil window, handle clicks, and open source windows."
              :source {:ns "uml-viewer.adapters.draw" :name "draw-state"}}
   :application {:text "The application layer generates IR from policy and source, then loads and navigates a document."
                 :source {:ns "uml-viewer.application.document" :name "load-path"}}
   :engine {:text "The engine compiles and lays out the current diagram view before it is drawn."
            :source {:ns "uml-viewer.engine.compose" :name "compile-diagram"}}
   :source {:text "The source boundary resolves a selected namespace or function to inspectable code."
            :source {:ns "uml-viewer.source" :name "member-source"}}
   :graph {:text "The graph boundary lets a language scanner report classes and dependencies to the generator."
           :source {:ns "uml-viewer.graph"}}
   :clojure-language {:text "The Clojure implementation scans namespace forms and locates selected functions in source files."
                      :source {:ns "uml-viewer.clojure-language.graph-clojure"}}
   :domain {:text "The domain rules apply policy, build hierarchy views, and keep authored proposals distinct from generated topology."
            :source {:ns "uml-viewer.domain.policy" :name "apply-policy"}}})

(def stages
  [{:id :read-and-scan
    :label "Read policy and scan source"
    :text "The generator reads the policy and asks the Clojure scanner for namespace topology. This is static source analysis, not a running call trace."
    :source {:ns "uml-viewer.application.ir-generator" :name "generate"}
    :participants [:main :application :graph :clojure-language]
    :functions [{:id :read-policy :label "read-policy"
                 :text "Reads the authored policy that controls generation."
                 :source {:ns "uml-viewer.application.ir-generator" :name "read-policy"}
                 :participants [:application]}
                {:id :parse-file :label "parse-file"
                 :text "Reads a Clojure file's namespace forms and reports its class and dependency facts to the scanner."
                 :source {:ns "uml-viewer.clojure-language.graph-clojure" :name "parse-file"}
                 :participants [:graph :clojure-language]}]
    :edges []}
   {:id :build-ir
    :label "Build and write IR"
    :text "The generator applies policy to the scanned graph and writes a generated EDN document. The file is the handoff to the viewer process."
    :source {:ns "uml-viewer.application.ir-generator" :name "generate"}
    :participants [:main :application :domain]
    :functions [{:id :generate :label "generate"
                 :text "Coordinates policy reading, scanning, policy application, and writing the EDN output."
                 :source {:ns "uml-viewer.application.ir-generator" :name "generate"}
                 :participants [:application]}
                {:id :apply-policy :label "apply-policy"
                 :text "Turns the scanned graph and authored policy into the hierarchical IR."
                 :source {:ns "uml-viewer.domain.policy" :name "apply-policy"}
                 :participants [:domain]}
                {:id :emit :label "emit"
                 :text "Serializes the generated document as EDN with a generated-file notice."
                 :source {:ns "uml-viewer.application.ir-generator" :name "emit"}
                 :participants [:application]}]
    :edges [[:generate :apply-policy] [:generate :emit]]}
   {:id :load-diagram
    :label "Load and compile diagram"
    :text "The viewer process reads the generated EDN and compiles the current hierarchy level into a drawable scene."
    :source {:ns "uml-viewer.application.document" :name "load-path"}
    :participants [:application :domain :engine]
    :functions [{:id :load-path :label "load-path"
                 :text "Loads the EDN document, overlays metrics, and creates the initial viewer state."
                 :source {:ns "uml-viewer.application.document" :name "load-path"}
                 :participants [:application]}
                {:id :load-document :label "load-document"
                 :text "Reads the EDN document that the generator wrote."
                 :source {:ns "uml-viewer.domain.ir" :name "load-document"}
                 :participants [:domain]}
                {:id :compile-view :label "compile-view"
                 :text "Builds the scene for the currently focused hierarchy level."
                 :source {:ns "uml-viewer.application.document" :name "compile-view"}
                 :participants [:application :engine]}]
    :edges [[:load-path :load-document] [:load-path :compile-view]]}
   {:id :draw-view
    :label "Draw current view"
    :text "Quil paints the compiled scene and Context on each frame. Drawing follows loading; it does not generate the IR."
    :source {:ns "uml-viewer.adapters.draw" :name "draw-state"}
    :participants [:adapters :engine]
    :functions [{:id :draw-state :label "draw-state"
                 :text "Draws the current scene and inspector in the viewer window."
                 :source {:ns "uml-viewer.adapters.draw" :name "draw-state"}
                 :participants [:adapters]}
                {:id :draw-edge :label "draw-edge"
                 :text "Draws a compiled relationship edge when the current view shows it."
                 :source {:ns "uml-viewer.adapters.draw" :name "draw-edge"}
                 :participants [:adapters]}
                {:id :draw-class :label "draw-class"
                 :text "Draws a class or component box from the compiled scene."
                 :source {:ns "uml-viewer.adapters.draw" :name "draw-class"}
                 :participants [:adapters]}]
    :edges [[:draw-state :draw-edge] [:draw-state :draw-class]]}])

(def file-handoff
  {:id :generated-edn :label "Generated EDN file"
   :text "The generator writes this file; a separate viewer process reads it. This arrow is a file handoff, not a direct function call."
   :file "examples/uml-viewer.edn"})

(defn available? [state]
  (let [doc (:doc state)
        path (:path state)]
    (and path
         (= (.getCanonicalFile (io/file path))
            (.getCanonicalFile (io/file "examples/uml-viewer.edn")))
         (= "UML viewer" (:title doc))
         (some #(= "uml-viewer.application.document" (:ns %)) (:classes doc))
         (some #(= "uml-viewer.main.ir-generator" (:ns %)) (:classes doc)))))

(defn stage [id]
  (some #(when (= id (:id %)) %) stages))

(defn function-step [id]
  (some (fn [s] (some #(when (= id (:id %)) %) (:functions s))) stages))

(defn item [id]
  (or (stage id) (function-step id)
      (when (= id :generated-edn) file-handoff)))

(defn visible-items [focus]
  (if-let [s (stage focus)]
    (:functions s)
    (vec (concat (take 2 stages) [file-handoff] (drop 2 stages)))))

(defn related-stages [structure-id]
  (let [n (some-> structure-id name)]
    (if n
      (->> stages
           (filter (fn [s]
                     (some #(or (= n (name %))
                                (str/starts-with? n (str (name %) ".")))
                           (:participants s))))
           (mapv :id))
      [])))

(defn structure-facts [doc id]
  (let [prefix (str (name id) ".")
        children (->> (:classes doc)
                      (map :id)
                      (filter #(str/starts-with? (name %) prefix))
                      (map #(first (str/split (subs (name %) (count prefix)) #"\.")))
                      distinct sort)
        deps (->> (:edges doc)
                  (keep (fn [{:keys [from to]}]
                          (when (= (first (str/split (name from) #"\.")) (name id))
                            (first (str/split (name to) #"\.")))))
                  (remove #(= % (name id))) distinct sort)
        source (get-in components [id :source :ns])]
    {:parent "repository root"
     :children children
     :dependencies deps
     :source (when source (str "src/" (str/replace source "." "/") ".clj"))}))

(defn selection-info [state]
  (when (available? state)
    (let [sel (:selected state)]
      (if (= :behavior (:kind sel))
        (item (:id sel))
        (get components (:id sel))))))

(defn context-action [state]
  (when (and (available? state) (nil? (:proposal-id state)))
    (let [sel (:selected state)]
      (if (= :behavior (:kind sel))
        (when (seq (:participants (selection-info state))) :show-in-structure)
        (when (seq (related-stages (:id sel))) :show-related-behavior)))))
