package chisellib.utils


import Chisel._

object Flow {
  def apply[T <: Data](gen: T): Flow[T] = new Flow(gen)
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

  def >->(next: ValidIO[T]) {
    val rValid = Reg(init = Bool(false))
    val rBits = Reg(gen)
    
    rValid := this.valid
    rBits := this.bits
    
    next.valid := rValid
    next.bits := rBits
  }
  def <-<(precedent: Flow[T]) {
    precedent >-> this
  }

  def &(linkEnable: Bool): Flow[T] = {
    val next = new Flow(this.bits)
    next.valid := this.valid && linkEnable
    next.bits := this.bits
    return next
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
