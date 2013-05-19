import java.util.List;
import java.util.LinkedList;

class Util {
  /**
  * http://stackoverflow.com/questions/8545590/java-find-the-most-popular-element-in-int-array
  */
  static int getPopularElement(Integer[] a) {
    int count = 1, tempCount;
    int popular = a[0];
    int temp = 0;
    for (int i = 0; i < (a.length - 1); i++) {
      temp = a[i];
      tempCount = 0;
      for (int j = 1; j < a.length; j++) {
        if (temp == a[j])
          tempCount++;
      }
      if (tempCount > count) {
        popular = temp;
        count = tempCount;
      }
    }
    return popular;
  }

  static int toDecade(int year) {
    return (year % 100) / 10;
  }

  static boolean isURI(String s) {
    return s.length() == 36 && s.substring(0, 14).equals("spotify:track:");
  }

  static Integer euclidDist(Node a, Node b) {
    return (int) Math.sqrt((b.x - a.x) ^ 2 + (b.y - a.y) ^ 2);
  }

  static List<String> nodesToCommands(List<Node> nodes) {
    List<String> commands = new LinkedList<String>();
    if (nodes.isEmpty()) {
      return commands;
    }

    Node p = nodes.get(0);
    for (Node n : nodes) {
      int x = p.x - n.x,
          y = p.y - n.y;
      p = n;

      if (x == 1) {
        commands.add("W");
      } else if (x == -1) {
        commands.add("E");
      } else if (y == 1) {
        commands.add("N");
      } else if (y == -1) {
        commands.add("S");
      }
    }

    return commands;
  }
}
