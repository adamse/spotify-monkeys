import java.util.Set;
import java.util.List;
import java.util.Map;
import java.util.HashSet;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Scanner;
import java.util.Collections;
import java.io.Serializable;
import java.io.ObjectOutputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.FileInputStream;

public class MonkeysBusiness {
  public static void main(String[] args) throws Exception {
    Scanner sc = new Scanner(System.in);
    String roundType = sc.nextLine();
    String id = sc.nextLine();
    Monkey monkey;
    if (roundType.equals("INIT")) {
      monkey = new Monkey(id);
      monkey.init(sc);
    } else {
      monkey = Monkey.readFromCache(id);
      monkey.turn(sc);
	  monkey.doTurn();
    }
    sc.close();
    monkey.writeToCache();
  }
}

class Monkey implements Serializable {
  public static final long serialVersionUID = 0;

  static final String CACHE_PREFIX = "cache_";

  final String id;
  // Position
  int x, y;
  // Toplists
  int topDecade;
  Set<String> topTracks = new HashSet<String>(),
              topAlbums = new HashSet<String>(),
              topArtists = new HashSet<String>(),
              dislikedArtists = new HashSet<String>();
  // URI dictionary
  Map<String, Track> knownURIs = new HashMap<String, Track>();
  // Tracks
  List<Track> unknownTracks;
  List<Track> knownTracks;
  // Path to walk
  List<String> pathList;
  List<Node> pathpathpath;
  Track targetTrack;
  int turn, turnLimit;
  int width, height;
  int remainingCapacity, remainingExecutionTime, boostCooldown;
  String[][] level;


  Monkey(String id) {
    this.id = id;
  }

  void init(Scanner sc) {
    parseInit(sc);
    parseToplists(sc);
  }

  void turn(Scanner sc) {
    parseTurn(sc);
    parseMetadata(sc);
    parseLevel(sc);
  }

  void parseInit(Scanner sc) {
    width = sc.nextInt();
    height = sc.nextInt();
    turnLimit = sc.nextInt();
    level = new String[width][height];
  }

  void parseToplists(Scanner sc) {
    List<Integer> decades = new ArrayList<Integer>();
    // Top tracks
    int numTracks = sc.nextInt();
    sc.nextLine();
    for (int i = 0; i < numTracks; i++) {
      String entry = sc.nextLine();
      // 0:[track],1:[album],2:[artist],3:[year]
      String[] parts = entry.split(",");
      topTracks.add(parts[0]);
      int decade = Util.toDecade(Integer.parseInt(parts[3]));
      decades.add(decade);
    }
    // Top albums
    int numAlbums = sc.nextInt();
    sc.nextLine();
    for (int i = 0; i < numAlbums; i++) {
      String entry = sc.nextLine();
      // 0:[album],1:[artist],2:[year]
      String[] parts = entry.split(",");
      topAlbums.add(parts[0]);
      int decade = Util.toDecade(Integer.parseInt(parts[2]));
      decades.add(decade);
    }
    // Top decade
    topDecade = Util.getPopularElement(
        decades.toArray(new Integer[decades.size()]));
    // Top artists
    int numArtists = sc.nextInt();
    sc.nextLine();
    for (int i = 0; i < numArtists; i++) {
      // 0:[artist]
      String entry = sc.nextLine();
      topArtists.add(entry);
    }
    // Disliked artists
    int numDislikedArtists = sc.nextInt();
    sc.nextLine();
    for (int i = 0; i < numDislikedArtists; i++) {
      // 0:[artist]
      String entry = sc.nextLine();
      dislikedArtists.add(entry);
    }
  }

  void parseTurn(Scanner sc) {
    turn = sc.nextInt();
    remainingCapacity = sc.nextInt();
    remainingExecutionTime = sc.nextInt();
    boostCooldown = sc.nextInt();
  }

  void parseMetadata(Scanner sc) {
    int numResults = sc.nextInt();
    sc.nextLine();
    for (int i = 0; i < numResults; i++) {
      String metadata = sc.nextLine();
      Track knownTrack = Track.fromMetadata(metadata);
      // 0:[uri],1:[[track],[album],[artist],[year]]
      String[] parts = metadata.split(",", 2); // Was 1, perhaps wrong
      knownURIs.put(parts[0], knownTrack);
    }
  }

  void parseLevel(Scanner sc) {
    unknownTracks = new ArrayList<Track>();
    knownTracks = new ArrayList<Track>();
    for (int y = 0; y < height; y++) {
      String row = sc.nextLine();
      String[] cells = row.split(",");
      for (int x = 0; x < width; x++) {
        level[x][y] = cells[x];
		if(cells[x].equals(id)) {
			this.x = x;
			this.y = y;
		}
        if (Util.isURI(cells[x])) {
          String uri = cells[x];
          if (!knownURIs.containsKey(uri)) {
            unknownTracks.add(new Track(uri).place(x, y));
          } else {
            knownTracks.add(knownURIs.get(uri).copy().place(x, y));
          }
        }
      }
    }

  }

  /* Finds the closest unkown track, using the euclidian distance */
  void getClosestTrack() {
    Track c = null;

    for (Track t : unknownTracks) {
      if (c == null) {
        c = t;
      } else {
        if (((t.x - x) ^ 2 + (t.y - y) ^ 2) < ((c.x - x) ^ 2 + (c.y - y) ^ 2)) {
          c = t;
        }
      }
    }

    targetTrack = c;
  }


  void doTurn() {
    /* Find the closest track if none is already found */
    if (targetTrack == null) {
      getClosestTrack();
      System.out.println(targetTrack.uri);
    } else if (pathList == null) {
      getPath(targetTrack);
      //getPath(targetTrack);
    }
	System.err.println("hej: " + pathList);
  }

  int trackTier() {
    Track t = knownURIs.get(targetTrack.uri);
    int tier = 0;

    if (dislikedArtists.contains(t.artist)) {
      return -2;
    } else if (topTracks.contains(t.name)) {
      return -1;
    }

    if (topArtists.contains(t.artist)) {
       tier++;
    }

    if (topAlbums.contains(t.album)) {
      tier++;
    }

    if (Util.toDecade(Integer.parseInt(t.year)) == topDecade) {
      tier++;
    }

    return tier;

  }

  void aStar() {
    pathpathpath = new LinkedList<Node>();
    Node start = new Node(x, y),
         goal = new Node(targetTrack.x, targetTrack.y);

    Set<Node> closedset = new HashSet<Node>(), // Evaluated nodes
              openset = new HashSet<Node>();   // Nodes to be evaluated
    openset.add(start);
    Map<Node, Node> came_from = new HashMap<Node, Node>(); // Navigated nodes

    Map<Node, Integer> g_score = new HashMap<Node, Integer>(), // Cost from start along best path
                    f_score = new HashMap<Node, Integer>();    // Estimated cost

    g_score.put(start, 0);
    f_score.put(start, g_score.get(start) + Util.euclidDist(start, goal));

    while (!openset.isEmpty()) {
      Node current = null;
      for (Node c : openset) {
        if (current == null) {
          current = c;
        } else if (f_score.get(c) < f_score.get(current)) {
          current = c;
        }
      }

      if (current.equals(goal)) {
        reconstruct_path(came_from, goal);
        //return reconstruct_pah(came_from, goal);
      }

      openset.remove(current);
      closedset.add(current);

      for (Node neighbour : getNeighborNodes(current)) {
        Integer tentative_g_score = g_score.get(current) + 1;

        if (closedset.contains(neighbour) && tentative_g_score >= g_score.get(neighbour)) {
          continue;
        }

        if (!openset.contains(neighbour) || tentative_g_score < g_score.get(neighbour)) {
          came_from.put(neighbour, current);
          g_score.put(neighbour, tentative_g_score);
          f_score.put(neighbour, g_score.get(neighbour) + Util.euclidDist(neighbour, goal));

          openset.add(neighbour);
        }
      }
    }
  }

  void reconstruct_path(Map<Node, Node> came_from, Node current_node) {
    if (came_from.containsKey(current_node)) {
      reconstruct_path(came_from, came_from.get(current_node));
      pathpathpath.add(current_node);
    } else {
      pathpathpath.add(current_node);
    }
  }

  void getPath(Track t) {
    pathList = new ArrayList<String>();

    Map<Node, Node> nextNodeMap = new HashMap<Node, Node>();
    Node sourceNode = new Node(x, y);
    Node destinationNode = new Node(t.x, t.y);
    Node currentNode = sourceNode;

    //Queue
    Queue<Node> queue = new LinkedList<Node>();
    queue.add(currentNode);

    Set<Node> visitedNodes = new HashSet<Node>();
    visitedNodes.add(currentNode);

	nextNodeMap.put(sourceNode, null);

    //Search.
    while (!queue.isEmpty()) {
      currentNode = queue.remove();
      if (currentNode.equals(destinationNode)) {
        break;
      } else {
        for (Node nextNode : getNeighborNodes(currentNode)) {
          if (!visitedNodes.contains(nextNode)) {
            queue.add(nextNode);
            visitedNodes.add(nextNode);

            nextNodeMap.put(nextNode, currentNode);
          }
        }
      }
    }
    for (Node node = destinationNode; node != null;) {
      Node next = nextNodeMap.get(node);
      if (next == null) break;

      if(next.x + 1 == node.x && next.y == node.y)
        pathList.add("E");
      else if(next.x - 1 == node.x && next.y == node.y)
        pathList.add("W");
      else if(next.x == node.x && next.y + 1 == node.y)
        pathList.add("S");
      else if(next.x == node.x && next.y - 1 == node.y)
        pathList.add("N");
	
	  node = next;
    }
    Collections.reverse(pathList);
	System.err.println(pathList);
  }

  List<Node> getNeighborNodes(Node current) {
    LinkedList<Node> ret = new LinkedList<Node>();
    int x = current.x,
        y = current.y;

	if ((x - 1 == targetTrack.y && y == targetTrack.x) || (x != 0 && level[x - 1][y].equals("_"))) {
		ret.add(new Node(x - 1, y));
	}
	if ((x + 1 == targetTrack.y && y == targetTrack.x) || (x <= height && level[x + 1][y].equals("_"))) {
		ret.add(new Node(x + 1, y));
	}
	if ((y - 1 == targetTrack.x && x == targetTrack.y) || (y != 0 && level[x][y - 1].equals("_"))) {
		ret.add(new Node(x, y - 1));
	}
	if ((y + 1 == targetTrack.x && x == targetTrack.y) || (y <= width && level[x][y + 1].equals("_"))) {
		ret.add(new Node(x, y + 1));
	}

    return ret;
  }

  void writeToCache() throws Exception {
    ObjectOutputStream out =
      new ObjectOutputStream(new FileOutputStream(CACHE_PREFIX + id));
    out.writeObject(this);
    out.close();
  }

  static Monkey readFromCache(String id) throws Exception {
    ObjectInputStream in =
      new ObjectInputStream(new FileInputStream(CACHE_PREFIX + id));
    Monkey monkey = (Monkey) in.readObject();
    in.close();
    return monkey;
  }

}

class Node {
  public int x, y;
  Node() {
    this.x = 0;
    this.y = 0;
  }
  Node(int a, int b) {
    this.x = a;
    this.y = b;
  }

  public boolean equals(Object o) {
    if (o == null) return false;
    if (o == this) return true;
    if (!(o instanceof Node)) return false;

    Node n = (Node) o;
    return this.x == n.x && this.y == n.y;
  }

  public int hashCode() {
    return (x + "," + y).hashCode();
  }
}

class Track implements Cloneable, Serializable {
  public static final long serialVersionUID = 0;

  String uri, name, album, artist, year;
  int x, y;
  int value;

  Track (String uri) {
    this.uri = uri;
  }

  static Track fromMetadata(String metadata) {
    // 0:[uri],1:[name],2:[album],3:[artist],4:[year]
    String[] parts = metadata.split(",");
    Track track = new Track(parts[0]);
    track.name = parts[1];
    track.album = parts[2];
    track.artist = parts[3];
    track.year = parts[4];
    return track;
  }

  Track place(int x, int y) {
    this.x = x;
    this.y = y;
    return this;
  }

  Track copy() {
    Track copy = new Track(uri);
    copy.name = name;
    copy.album = album;
    copy.artist = artist;
    copy.year = year;
    copy.value = value;
    return copy;
  }

  @Override public int hashCode() { return uri.hashCode(); }
}

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
}
