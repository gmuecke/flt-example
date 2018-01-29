public class Main {

  /**
   * This code doesn't work, see
   *
   * <a href="http://www.google.ch/search?q='\u002a\u002f\u000dstatic{int\u0020i=0/0;}\u002f\u002a'">here</a>
   */
  public static void main(String... args) {
    System.out.println("Hello world");
  }
}
