package fr.delthas.skype;

/**
 * An immutable generic tuple of two elements (pair).
 *
 * @param <T> The type of the first element.
 * @param <U> The type of the second element.
 *
 */
public class Pair<T, U> {

  private final T first;
  private final U second;

  /**
   * @param first The first element of the pair.
   * @param second The second element of the pair.
   */
  public Pair(T first, U second) {
    this.first = first;
    this.second = second;
  }

  /**
   * @return The first element of the pair.
   */
  public T getFirst() {
    return first;
  }

  /**
   * @return The second element of the pair.
   */
  public U getSecond() {
    return second;
  }

}
