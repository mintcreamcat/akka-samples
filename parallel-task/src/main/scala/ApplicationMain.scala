import akka.actor._

object ApplicationMain extends App {
  val system = ActorSystem("MyActorSystem")
  val taskExecutor = system.actorOf(Props[ToUpperCaseParallelTaskExecutionActor])
  taskExecutor ! new ToUpperCaseRequest(Set("select", "insert", "update", "delete"))
  Thread.sleep(3000)
  system.shutdown()
}

class ToUpperCaseParallelTaskExecutionActor extends Actor {

  def receive: Receive = {
    case request: ToUpperCaseRequest => {
      // 要素1つごとに1つ子アクターを生成してタスクを実行させる
      // これらの子アクターは並列で実行される
      request.textSet.foreach(text => {
        val task = this.context.actorOf(Props[ToUpperCaseTaskActor])
        // 子アクターを監視
        context.watch(task)
        task ! text
      })
      this.context.become(waitEnd)
    }
  }

  def waitEnd: Receive = {
    case Terminated(child) => {
      // 全ての子アクターが終了したら自身を終了させる
      if (this.context.children.count(a => true) == 0) this.context.stop(self)
    }
  }
}

class ToUpperCaseRequest(val textSet: Set[String])

class ToUpperCaseTaskActor extends Actor {
  def receive: Receive = {
    case text: String => {
      println(text.toUpperCase)
      // 自身を終了させる
      context.stop(self)
    }
  }
}
