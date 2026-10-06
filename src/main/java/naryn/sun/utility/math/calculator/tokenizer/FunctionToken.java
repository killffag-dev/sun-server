package naryn.sun.utility.math.calculator.tokenizer;

import naryn.sun.utility.math.calculator.function.Function;

public class FunctionToken extends Token {
   private final Function function;

   public FunctionToken(Function function) {
      super(3);
      this.function = function;
   }

   public Function getFunction() {
      return this.function;
   }
}
