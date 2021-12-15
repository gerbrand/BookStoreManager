package xls

import org.apache.poi.ss.usermodel.{RichTextString, Row}
import org.apache.poi.xssf.usermodel.XSSFRow

import java.util.{Calendar, Date}

object PoiUtils {
  def updateCellIfNeeded[T](row: XSSFRow, cellNum: Int, value: Option[T])  = {
    value.foreach(v => {
      val cell = row.getCell(cellNum, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK)
      if (cell.getStringCellValue!=v.toString) {
        /** Deconstruct based on type. */
        v match {
          case v:Seq[String] =>cell.setCellFormula(v.mkString("\"",",",""))
          case v:String =>cell.setCellValue(v)
          case v:Double =>cell.setCellValue(v)
          case v:Calendar =>cell.setCellValue(v)
          case v:RichTextString =>cell.setCellValue(v)
          case v:Date =>cell.setCellValue(v)
          case v:Boolean => cell.setCellValue(v)
          case v => cell.setCellValue(s"$v")
        }
      }
    })
  }
}
