# clj-scientist

[![Clojars Project](https://img.shields.io/clojars/v/clj-scientist.svg)](https://clojars.org/clj-scientist)

clj-scientist is a Clojure library ported from github's ruby library 
[scientist](https://github.com/github/scientist), which allow you to 
implement [Branch by Abstraction][1].

Please refer to the excellent post [Scientist: Measure Twice, Cut Over Once][2]
and the [original library][3] for the core idea of the library.

## Usage

For leiningen, add the following to your `project.clj` file

```
[clj-scientist "0.1.0-SNAPSHOT"]
```

For maven project, please add the following to your `pom` file

```
<dependency>
  <groupId>clj-scientist</groupId>
  <artifactId>clj-scientist</artifactId>
  <version>0.1.0-SNAPSHOT</version>
</dependency>
```

Some example usage as follows:

```clojure
(ns demo.core
  (require [clj-scientist.core :as sci]
           [clj-scientist.publisher :as pub]))


(defn sum-range-naive
  "sum 1 + 2 + ... + n naively"
  [n]
  (reduce + (range 1 (inc n))))


(defn sum-range-math
  "sum 1 + 2 + ... + n use formula n * (n + 1) / 2"
  [n]
  (quot (* n (inc n)) 2))


(defn sum-range-math-wrong
  "sum 1 + 2 + ... + n use formula n * (n - 1) / 2"
  [n]
  (quot (* n (dec n)) 2))

;; test for two equal function, use logger-everything to print
;; the logger info to see the difference evluation. context
;; is used to store useful information for debugging.
(defn test-two-functions
  [n]
  (sci/experiment (sci/new-config {:arg n} pub/logger-everything)
                  (fn [] (sum-range-naive n)) 
                  (fn [] (sum-range-math n))))


;; test for more than two function, use logger-only-mismatch
(defn test-more-than-two-functions
  [n]
  (sci/experiment (sci/new-config {:arg n})
                  (fn [] (sum-range-naive n)) 
                  (fn [] (sum-range-math n))
                  (fn [] (sum-range-math-wrong n))))
```

The `clj-scientist.core/experiment` will take a configuration, one 
control function and more than one experiment function as its input,
it will evaluation all the function in random order(shuffle to avoid
efficiency of order issue), record the metrics and publish it(User 
need to implement their own publisher function if they need, in the
file `src/clj_scientist/publisher.clj`, there are just two logging 
publisher for convenient use). 

Some call example:

```
demo.core=> (test-two-function 10000)
Feb 21, 2016 1:26:23 PM clojure.tools.logging$eval406$fn__410 invoke
INFO: {"context":{"arg":10000},"match":true,"execution_order":["control",\
"experiment-01"],"time-stamp":1456032383,"metrics":[{"name":"control",\
"exception":false,"result":50005000,"duration":5.622544}, \
{"name":"experiment-01","exception":false,"result":50005000,\
"duration":0.075933}]}
50005000

demo.core=> (test-more-than-two-functions 10000)
Feb 21, 2016 1:29:31 PM clojure.tools.logging$eval406$fn__410 invoke
SEVERE: [MISMATCH]: {"context":{"arg":10000},"match":false,"execution_order":\
["control","experiment-01","experiment-02"],"time-stamp":1456032571,"metrics":\
[{"name":"control","exception":false,"result":50005000,"duration":1.970213},\
{"name":"experiment-01","exception":false,"result":50005000,"duration":\
0.099817},{"name":"experiment-02","exception":false,"result":49995000,\
"duration":0.073729}]}
50005000
```

The `new-config` function take a hash-map of context for debugging 
purpose and a function which is the customized publisher, you can 
also use the default setting.

The `clj-scientist` can also handle exception well, if two function
throw the **same exception**, it will regard it as a match result, 
like the following example.

The following are some example, if the control and experiment function
do throw the same exception, it will not consider it as a mismatch

```
demo.core=> (sci/experiment (sci/new-config) (fn [] (/ 1 0)) (fn [] (/ 1 0)))
ArithmeticException Divide by zero  clojure.lang.Numbers.divide \
(Numbers.java:158)
```

But if they throw different exception, it will consider it as a mismatch.

```
demo.core=> (sci/experiment (sci/new-config) (fn [] (/ 1 0)) (fn [] \
(throw (Exception. "hello"))))
Feb 21, 2016 1:41:13 PM clojure.tools.logging$eval406$fn__410 invoke
SEVERE: [MISMATCH]: {"context":{},"match":false,"execution_order":\
["control","experiment-01"],"time-stamp":1456033273,"metrics":\
[{"name":"control","exception":"java.lang.ArithmeticException","result":\
null,"duration":0.581191},{"name":"experiment-01","exception":\
"java.lang.Exception","result":null,"duration":0.137101}]}
ArithmeticException Divide by zero  clojure.lang.Numbers.divide \
(Numbers.java:158)
```

Finally, it will return the result of the control function, if the 
control function throw a exception, it will re-throw the exception.



## License

Copyright © 2016 m00nlight

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.


[1]: http://martinfowler.com/bliki/BranchByAbstraction.html
[2]: http://githubengineering.com/scientist/
[3]: https://github.com/github/scientist
