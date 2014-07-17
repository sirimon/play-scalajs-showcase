package example

import org.scalajs.dom
import scala.scalajs.js
import scala.scalajs.js.annotation.JSExport
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import js.Dynamic.{global => g, _}
import scalatags.JsDom._
import all._
import org.scalajs.jquery.{jQuery=>$,_}

@JSExport
object ScalaJSFileUpload {

  def markup(csrfToken: String) = div(
    h1("HTML5 File Drag &amp; Drop API"),
    p("""This is a demonstration of the HTML5 file drag &amp; drop API with asynchronous Ajax file uploads,
      graphical progress bars and progressive enhancement."""),
    form(id:= "upload", action:=s"/upload",
      target:="", "method".attr:="POST", "enctype".attr:="multipart/form-data"){
      fieldset (
        legend("HTML File Upload"),
        input(`type`:="hidden", name:="csrfToken", value:=csrfToken),
        div (
          label(`for` := "", "Files to upload:"),
          input(`type` := "file", id := "fileSelect", name := "fileSelect", "multiple".attr := "multiple"),
          div(id := "fileDrag", backgroundColor:= "green")("or drop files here")
        ),
        div(id := "submitButton") (
          button(`type` := "submit")("Upload Files")
        )
      )
    },
    div(id:="progress"),
    div(id:="messages")(p("Status Messages")),
    br,
    h2("Disclaimer"),
    p("The original code was developed by ", a(href:="http://twitter.com/craigbuckler", "Craig Buckler"), " of ",
      a(href:="http://optimalworks.net/", "OptimalWorks.net"), " for ",
      a(href:="http://sitepoint.com/", "SitePoint.com"), ".")
  )

  trait EventTargetExt extends dom.EventTarget {
    var files: dom.FileList = ???
    var className: String = ???
    var result: js.Any = ???

  }
  trait EventExt extends dom.Event {
    var dataTransfer: dom.DataTransfer = ???

    var loaded: Int = ???
    var total: Int = ???
  }

  def scripts = {
    implicit def monkeyizeEventTarget(e: dom.EventTarget): EventTargetExt = e.asInstanceOf[EventTargetExt]
    implicit def monkeyizeEvent(e: dom.Event): EventExt = e.asInstanceOf[EventExt]

    val maxFileSize = 3000000l

    def $id(s: String) = dom.document.getElementById(s)

    def output(msg: String) = {
      val m = $id("messages")
      m.innerHTML = msg + m.innerHTML
    }

    def fileDragHover(e: dom.Event) = {
      e.stopPropagation()
      e.preventDefault()
      e.target.className = if(e.`type` == "dragover") "hover" else ""
    }

    def fileSelectHandler(e: dom.Event) = {
      fileDragHover(e)
      val files = if(e.target.files.toString != "undefined") {
        e.target.files
      } else {
        e.asInstanceOf[dom.DragEvent].dataTransfer.files
//        e.dataTransfer.files
      }
//      dom.alert(files.toString)
//      dom.alert((files != null).toString)
      (0 until files.length).foreach{ i =>
//        dom.alert(files(i).toString)
        try {
          parseFile(files(i))
          uploadFile(files(i))
        }catch{
          case e:Throwable => println(e)
        }
      }
    }

    def parseFile(file: dom.File) = {
      output(
        s"""
          |<p>File information: <strong>${file.name}</strong>
          | type: <strong>${file.`type`}</strong>
          | size: <strong>${file.size}</strong> bytes</p>
        """.stripMargin)
      val reader = new FileReader()
      if(file.`type`.indexOf("image") == 0) {
        reader.onload = (e: dom.Event) => {
          output(
            s"""
              |<p><strong>${file.name}:</strong><br />
              |<img src=""/>${e.target.result}</p>
            """.stripMargin)
        }
        reader.readAsDataURL(file)
      }else if(file.`type`.indexOf("text") == 0){
        reader.onload = (e: dom.Event) => {
          output(
            s"""
              |<p><strong>${file.name}:</strong></p>
              |<pre>${e.target.result}</pre>
            """.stripMargin)
          reader.readAsText(file)
        }
      }
    }

    def uploadFile(file: dom.File) = {
      val xhr = new dom.XMLHttpRequest
      if(xhr.upload != null && file.size <= maxFileSize){
        val o = $id("progress")
        val progress = o.appendChild(dom.document.createElement("p")).asInstanceOf[dom.HTMLElement]
        progress.appendChild(dom.document.createTextNode(s"upload ${file.name}"))

        xhr.upload.addEventListener("progress", (e: dom.Event) => {
          val pc = 100 - (e.loaded / e.total * 100)
          progress.style.backgroundPosition = s"$pc % 0"
        }, false)

        xhr.onreadystatechange = (e: dom.Event) => {
          if(xhr.readyState == 4){
            progress.className = if(xhr.status == 200) "success" else "failure"
          }
        }
        //start upload
        xhr.open("POST", $id("upload").asInstanceOf[dom.HTMLFormElement].action, true)
        xhr.setRequestHeader("X-Request-With", "XMLHttpRequest")
        xhr.setRequestHeader("X-FILENAME", file.name)
        xhr.send(file)
      }
    }

    def init = {

      $("#fileDrag").on("dragenter dragstart dragend dragleave dragover drag drop", (e: dom.Event) => {
        e.preventDefault();
      });

      $id("fileSelect").addEventListener("change", fileSelectHandler _, false)
      $id("fileSelect").ondragend
      val xhr = new dom.XMLHttpRequest
      if(xhr.upload != null){
        val fileDrag = $id("fileDrag")
        fileDrag.addEventListener("dragover", fileDragHover _, false)
        fileDrag.addEventListener("dragleave", fileDragHover _, false)
        fileDrag.addEventListener("drop", fileSelectHandler _, false)
        fileDrag.style.display = "block"

        $id("submitButton").style.display = "none"
      }
    }

    init
  }

  @JSExport
  def main(csrfToken: String) = {
    dom.document.body.innerHTML = ""
    dom.document.body.appendChild(markup(csrfToken).render)
    scripts
  }
}

object FileReader extends js.Object {
  //states
  val EMPTY: Short = 0
  val LOADING: Short = 1
  val DONE: Short = 2
}

class FileReader() extends dom.EventTarget {
  import dom._

  /**
   * A DOMError representing the error that occurred while reading the file.
   *
   * MDN
   */
  val error: DOMError = ???

  /**
   * A number indicating the state of the FileReader. This will be one of the State constants.
   * EMPTY   : 0 : No data has been loaded yet.
   * LOADING : 1 : Data is currently being loaded.
   * DONE    : 2 : The entire read request has been completed.
   *
   * MDN
   */
  val readyState: Short = ???

  /**
   * The file's contents. This property is only valid after the read operation is
   * complete, and the format of the data depends on which of the methods was used to
   * initiate the read operation.
   *
   * MDN
   */
  val result: js.Any = ???

  /**
   * A handler for the abort event. This event is triggered each time the reading
   * operation is aborted.
   *
   * MDN
   */
  var onabort: js.Function1[Event, _] = ???

  /**
   * A handler for the error event. This event is triggered each time the reading
   * operation encounter an error.
   *
   * MDN
   */
  var onerror: js.Function1[Event, _] = ???

  /**
   * A handler for the load event. This event is triggered each time the reading
   * operation is successfully completed.
   *
   * MDN
   */
  var onload: js.Function1[UIEvent, _] = ???

  /**
   * A handler for the loadstart event. This event is triggered each time the reading
   * is starting.
   *
   * MDN
   */
  var onloadstart: js.Function1[ProgressEvent, _] = ???

  /**
   * A handler for the loadend event. This event is triggered each time the reading
   * operation is completed (either in success or failure).
   *
   * MDN
   */
  var onloadend: js.Function1[ProgressEvent, _] = ???

  /**
   * A handler for the progress event. This event is triggered while reading
   * a Blob content.
   *
   * MDN
   */
  var onprogress: js.Function1[ProgressEvent, _] = ???

  /**
   * Aborts the read operation. Upon return, the readyState will be DONE.
   *
   * MDN
   */
  def abort(): Unit = ???

  /**
   * The readAsArrayBuffer method is used to starts reading the contents of the
   * specified Blob or File. When the read operation is finished, the readyState
   * becomes DONE, and the loadend is triggered. At that time, the result attribute
   * contains an ArrayBuffer representing the file's data.
   *
   * MDN
   */
  def readAsArrayBuffer(blob: Blob): Unit = ???

  /**
   * The readAsDataURL method is used to starts reading the contents of the specified
   * Blob or File. When the read operation is finished, the readyState becomes DONE, and
   * the loadend is triggered. At that time, the result attribute contains a data: URL
   * representing the file's data as base64 encoded string.
   *
   * MDN
   */
  def readAsDataURL(blob: Blob): Unit = ???

  /**
   * The readAsText method is used to read the contents of the specified Blob or File.
   * When the read operation is complete, the readyState is changed to DONE, the loadend
   * is triggered, and the result attribute contains the contents of the file as a text string.
   *
   * MDN
   */
  def readAsText(blob: Blob, encoding: String = "UTF-8"): Unit = ???

  /**
   * The readAsText method is used to starts reading the contents of the specified Blob
   * or File. When the read operation is finished, the readyState becomes DONE, and the
   * loadend is triggered. At that time, the result attribute contains the contents of
   * the file as a text string.
   *
   * MDN
   */
  def readAsText(blob: Blob): Unit = ???

}
