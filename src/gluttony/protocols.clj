(ns gluttony.protocols)

(defprotocol IConsumer
  (-start [this])
  (-stop [this]))
