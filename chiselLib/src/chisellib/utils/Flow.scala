package chisellib.utils

import Chisel._

object Flow {
  def apply[T <: Data](gen: T): Flow[T] = new Flow(gen)
  def apply[T <: Data](valid: Bool, bits: T): Flow[T] = {
    val flow = new Flow(bits)
    flow.valid := valid
    flow.bits := bits
    flow
  }
}

class Flow[T <: Data](gen: T) extends ValidIO(gen) {
  def asMaster(dummy: Int = 0): Flow.this.type = { Flow.this }
  def asSlave(dummy: Int = 0): Flow.this.type = { flip; Flow.this }

  def >>(next: ValidIO[T]) {
    next.valid := valid
    next.bits := bits
  }
  def <<(next: ValidIO[T]) {
    valid := next.valid
    bits := next.bits
  }

  def <<(next: FlowReg[T]) {
    valid := next.valid
    bits := next.bits
  }

  def <<(next: DecoupledIO[T]) {
    valid := next.valid
    bits := next.bits
    next.ready := Bool(true)
  }

  def >->(next: ValidIO[T], clk: Clock = null) {
    val rValid = Reg(init = Bool(false), clock = clk)
    val rBits = Reg(gen, null.asInstanceOf[T], null.asInstanceOf[T], clock = clk)

    rValid := this.valid
    rBits := this.bits

    next.valid := rValid
    next.bits := rBits
  }
  def <-<(precedent: Flow[T], clk: Clock = null) {
    precedent >-> (this, clk)
  }

  def &(linkEnable: Bool): Flow[T] = {
    val next = new Flow(this.bits)
    next.valid := this.valid && linkEnable
    next.bits := this.bits
    return next
  }
  def ~[T2 <: Data](nextBits: T2): Flow[T2] = {
    val next = new Flow(nextBits)
    next.valid := this.valid
    next.bits := nextBits
    next
  }
  def toRegMemory(init: T = null.asInstanceOf[T]): T = {
    val reg = if (init == null) Reg(gen) else RegInit(init)
    when(this.valid) {
      reg := bits
    }
    reg
  }
  def ff(dummy : Int = 0) = {
    val next = clone
    next <-< this
    next
  }

  override def clone: Flow.this.type =
    try {
      super.clone()
    } catch {
      case e: java.lang.Exception => {
        (new Flow(gen)).asInstanceOf[Flow.this.type]
      }
    }
}

class FlowReg[T <: Data](gen: T) extends Bundle {

  val valid = Reg(init = Bool(false))
  val bits = Reg(gen)

  def >>(next: ValidIO[T]) {
    next.valid := valid
    next.bits := bits
  }

  def >>(next: FlowReg[T]) {
    next.valid := valid
    next.bits := bits
  }

  override def clone: this.type = { new FlowReg(gen).asInstanceOf[this.type]; }
}
