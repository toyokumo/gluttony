(ns gluttony.protocols)

(defprotocol IConsumer
  (-start [this])
  (-stop [this])
  (-enable-receivers [this])
  (-disable-receivers [this]))
