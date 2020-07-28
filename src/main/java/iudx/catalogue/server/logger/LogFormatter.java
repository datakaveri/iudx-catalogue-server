package iudx.catalogue.server.logger;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

public class LogFormatter extends Formatter {

  @Override
  public String format(LogRecord record) {
    StringBuilder sb = new StringBuilder();
    sb.append(calcDate(record.getMillis()))
      .append(" ")
      .append("[" + record.getLevel().toString() + "]")
      .append("[" + record.getSourceClassName() + "]")
      .append("[" + record.getSourceMethodName() + "]")
      .append(" ")
      .append(record.getMessage());
    return sb.toString();
  }

  private String calcDate(long millisecs) {
    SimpleDateFormat date_format = new SimpleDateFormat("MMM dd,yyyy HH:mm");
    Date resultdate = new Date(millisecs);
    return date_format.format(resultdate);
  }
}
