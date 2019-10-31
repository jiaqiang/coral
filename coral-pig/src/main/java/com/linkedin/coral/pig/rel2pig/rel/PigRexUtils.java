package com.linkedin.coral.pig.rel2pig.rel;

import com.linkedin.coral.pig.rel2pig.rel.operators.PigBinaryOperator;
import com.linkedin.coral.pig.rel2pig.rel.operators.PigOperator;
import com.linkedin.coral.pig.rel2pig.rel.operators.PigPrefixOperator;
import com.linkedin.coral.pig.rel2pig.rel.operators.PigSpecialOperator;
import java.util.List;
import org.apache.calcite.rex.RexCall;
import org.apache.calcite.rex.RexInputRef;
import org.apache.calcite.rex.RexLiteral;
import org.apache.calcite.rex.RexNode;
import org.apache.calcite.sql.SqlBinaryOperator;
import org.apache.calcite.sql.SqlPrefixOperator;
import org.apache.calcite.sql.SqlSpecialOperator;


/**
 * PigRexUtils provides utilities to translate SQL expressions represented as
 * Calcite RexNode into Pig Latin.
 */
public class PigRexUtils {

  private PigRexUtils() {

  }

  /**
   * Transforms a SQL expression represented as a RexNode to equivalent Pig Latin
   *
   * @param rexNode RexNode SQL expression to be transformed
   * @param inputFieldNames Column name accessors for input references
   * @return Pig Latin equivalent of given rexNode
   */
  public static String convertRexNodeToPigExpression(RexNode rexNode, List<String> inputFieldNames) {
    if (rexNode instanceof RexInputRef) {
      return convertRexInputRef((RexInputRef) rexNode, inputFieldNames);
    } else if (rexNode instanceof RexCall) {
      return convertRexCall((RexCall) rexNode, inputFieldNames);
    } else if (rexNode instanceof RexLiteral) {
      return convertRexLiteral((RexLiteral) rexNode);
    }
    return rexNode.toString();
  }

  /**
   * Resolves the Pig Latin accessor name of an input reference given by a RexInputRef
   *
   * @param rexInputRef Input reference to be resolved
   * @param inputFieldNames Mapping from list index to accessor name
   * @return Pig Latin accessor name of the given rexInputRef
   */
  private static String convertRexInputRef(RexInputRef rexInputRef, List<String> inputFieldNames) {
    if (rexInputRef.getIndex() >= inputFieldNames.size()) {
      //TODO(ralam): Create better exception messages
      throw new RuntimeException(String.format(
          "RexInputRef failed to access field at index %d with RexInputRef column name mapping of size %d",
          rexInputRef.getIndex(), inputFieldNames.size()));
    }
    return inputFieldNames.get(rexInputRef.getIndex());
  }

  /**
   * Resolves the Pig Latin literal for a RexLiteral
   *
   * @param rexLiteral RexLiteral to be resolved
   * @return Pig Latin literal of the given rexLiteral
   */
  private static String convertRexLiteral(RexLiteral rexLiteral) {
    switch (rexLiteral.getTypeName()) {
      case CHAR:
        return String.format("'%s'", rexLiteral.toString());
      default:
        return rexLiteral.toString();
    }
  }

  /**
   * Resolves the Pig Latin expression for a SQL expression given by a RexCall.
   *
   * @param rexCall RexCall to be resolved
   * @param inputFieldNames Mapping from list index to accessor name
   * @return Pig Latin expression of the given rexCall
   */
  private static String convertRexCall(RexCall rexCall, List<String> inputFieldNames) {
    // TODO(ralam): Add more supported RexCall functions.
    PigOperator pigOperator = null;

    if (rexCall.getOperator() instanceof SqlSpecialOperator) {
      pigOperator = new PigSpecialOperator(rexCall, inputFieldNames);
    } else if (rexCall.getOperator() instanceof SqlBinaryOperator) {
      pigOperator = new PigBinaryOperator(rexCall, inputFieldNames);
    } else if (rexCall.getOperator() instanceof SqlPrefixOperator) {
      pigOperator = new PigPrefixOperator(rexCall, inputFieldNames);
    } else {
      // TODO(ralam): Finish implementing RexCall resolution. Throw an unsupported exception in the meantime.
      throw new UnsupportedOperationException(
          String.format("Unsupported operator: %s", rexCall.getOperator().getName()));
    }

    return pigOperator.unparse();
  }

}