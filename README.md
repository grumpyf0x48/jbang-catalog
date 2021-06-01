# jbang-catalog

My collection of [jbang](https://www.jbang.dev) scripts.

## Install jbang

```console
$ curl -Ls https://sh.jbang.dev | bash -s -
```

Then:

```console
$ export PATH=$HOME/.jbang/bin:$PATH
```

## Ssh

A basic SSH client.

Written with Apache Mina, jbang and Picocli.

```console
$ jbang ssh@grumpyf0x48 --help
Usage: Ssh [-hV] [-a=<authenticationTimeout>] [-c=<connectionTimeout>]
           [-i=<identityFile>] [-p=<password>] [--port=<port>] <destination>
           [<command>...]
A basic SSH client
      <destination>    Destination to reach (format: user@hostname)
      [<command>...]   Command to execute and its parameters
  -a, --authentication-timeout=<authenticationTimeout>
                       Authentication timeout in seconds (default: 30)
  -c, --connection-timeout=<connectionTimeout>
                       Connection timeout in seconds (default: 30)
  -h, --help           Show this help message and exit.
  -i, --identity=<identityFile>
                       Identity file
  -p=<password>        Password
      --port=<port>    Port (default: 22)
  -V, --version        Print version information and exit.
```

### Sample use

```console
$ jbang ssh@grumpyf0x48 -i ~/.ssh/id_rsa pi@192.168.0.108 ls /home
```

## Sort

A sample sort program to illustrate Java stream and Array sort use.

Written with jbang and Picocli.

```console
$ jbang sort@grumpyf0x48 --help
Usage: Sort [-himnrV] [<elements>...]
A sample sort program to illustrate Java stream and Array sort use
      [<elements>...]        Elements to sort
  -h, --help                 Show this help message and exit.
  -i, --ignore-case          Enable ignore case
  -m, --human-numeric-sort   Enable human numeric sort
  -n, --numeric-sort         Enable numeric sort
  -r, --reverse              Enable reverse sort
  -V, --version              Print version information and exit.
```

### Sample use

```console
$ jbang sort@grumpyf0x48 -r -m $(exa -l ~/Downloads | cut -d " " -f 2 | xargs)
```

## Links

A program to retrieve the posts metadata of my former blog archived on https://web.archive.org.

Written with Jsoup, Gson, jbang and Picocli.

```console
$ jbang links@grumpyf0x48 --help
Usage: Links [-hvV] [-a=<archivedSiteUrl>] [-f=<firstPage>] [-l=<lastPage>]
             [-r=<rootUrl>] <file>
A program to retrieve the posts metadata of my former blog archived on https://web.archive.org
      <file>                 The output file
  -a, --archived-site-url=<archivedSiteUrl>
                             The URL of the archived web Site (default: http:
                               //blog.onkeyboardst.net)
  -f, --first-page=<firstPage>
                             First page to parse (default: 1)
  -h, --help                 Show this help message and exit.
  -l, --last-page=<lastPage> Last page to parse (default: 1)
  -r, --root-url=<rootUrl>   The root URL to parse on https://web.archive.org
                               (default: https://web.archive.
                               org/web/20200211042437)
  -v, --verbose              Enable verbose mode
  -V, --version              Print version information and exit.
```

### Sample use

```console
$ jbang links@grumpyf0x48 -v -f 1 -l 4 firstPosts.json
```

## Html

A program to build the archive page for my new blog from the posts.json file generated by Links.

Written with Gson, jbang and Picocli.

```console
$ jbang html@grumpyf0x48 --help
Usage: Html [-hV] -i=<inputFile> -o=<outputFile>
A program to build the archive page for my new blog from the posts.json file generated by Links
  -h, --help         Show this help message and exit.
  -i=<inputFile>     The input file (.json)
  -o=<outputFile>    The output file (.html)
  -V, --version      Print version information and exit.
```

### Sample use

```console
$ jbang html@grumpyf0x48 -i posts.json -o archive.html
```

## SizeOf

A program to display the memory size of Java primitive types and their corresponding wrapper classes.

Written with OpenJDK jol, jbang and Picocli.

```console
$ jbang sizeof@grumpyf0x48 --help
Usage: SizeOf [-hV] [<types>...]
A program to display the memory size of Java primitive types and their corresponding wrapper classes
      [<types>...]   The primitive types (default: byte, boolean, char, short,
                       int, float, long, double
  -h, --help         Show this help message and exit.
  -V, --version      Print version information and exit.
```

### Sample use

```console
$ jbang sizeof@grumpyf0x48
sizeof(byte)=1, sizeof(Byte)=16
sizeof(boolean)=1, sizeof(Boolean)=16

sizeof(char)=2, sizeof(Character)=16
sizeof(short)=2, sizeof(Short)=16

sizeof(int)=4, sizeof(Integer)=16
sizeof(float)=4, sizeof(Float)=16

sizeof(long)=8, sizeof(Long)=24
sizeof(double)=8, sizeof(Double)=24
```

## GitClone

A basic `git clone` replacement in Java.

Written with jGit, jbang and Picocli.

```console
$ jbang git-clone@grumpyf0X48 --help
Usage: GitClone [-hnV] [--bare] [-b=<branch>] [-i=<identityFile>] <repository>
                [<directory>]
A basic `git clone` replacement in Java
      <repository>        The repository to clone from
      [<directory>]       The name of a new directory to clone into
  -b, --branch=<branch>   Branch name
      --bare              Make a bare Git repository
  -h, --help              Show this help message and exit.
  -i, --identity=<identityFile>
                          Identity file in PEM format (default: ~/.ssh/id_rsa)
  -n, --no-checkout       No checkout of HEAD is performed after the clone is
                            complete
  -V, --version           Print version information and exit.
```

The SSH key should be in PEM format.

To convert a private key from OPENSSH to PEM format:

```console
$ ssh-keygen -p -f ~/.ssh/id_rsa -m pem
```

### Sample use

```console
$ jbang git-clone@grumpyf0x48 git@github.com:jbangdev/jbang.git /tmp/jbang
```

## GitGet

A command to get one or more files (or directories) from a Git repository.

Written with jGit, jbang and Picocli

It is like `git clone` but restricted to some files or directories of the repository.

```console
$ jbang git-get@grumpyf0x48 --help
Usage: GitGet [-hV] [--fresh] [-b=<branch>] [-i=<identityFile>] <repository>
              <directory> <paths>...
A command to get one or more files (or directories) from a Git repository
      <repository>        The repository to clone from
      <directory>         The name of a new directory where to store files
      <paths>...          The file or directory paths to get from the repository
  -b, --branch=<branch>   Branch name
      --fresh             Make a fresh clone of the repository
  -h, --help              Show this help message and exit.
  -i, --identity=<identityFile>
                          Identity file in PEM format (default: ~/.ssh/id_rsa)
  -V, --version           Print version information and exit.
```

### Sample use

```console
$ jbang git-get@grumpyf0x48 git@github.com:jbangdev/jbang.git ~/jbang-files misc/ src/ readme.adoc
$ ls -l ~/jbang-files
total 64
drwxrwxr-x 3 user user  4096 mars   7 08:24 misc
-rw-rw-r-- 1 user user 54560 mars   7 08:24 readme.adoc
drwxrwxr-x 4 user user  4096 mars   7 08:24 src
```

Need to get more files ?

```console
$ jbang git-get@grumpyf0x48 git@github.com:jbangdev/jbang.git ~/jbang-files examples/ LICENSE
$ ls -l ~/jbang-files
total 72
drwxrwxr-x 2 user user  4096 mars   7 08:24 examples
-rw-rw-r-- 1 user user  1076 mars   7 08:24 LICENSE
drwxrwxr-x 3 user user  4096 mars   7 08:24 misc
-rw-rw-r-- 1 user user 54560 mars   7 08:24 readme.adoc
drwxrwxr-x 4 user user  4096 mars   7 08:24 src
```

The previously cloned repository in `System.getProperty("java.io.tmpdir")` will then be re-used.

## WhatsNewInJava

A program to list new methods added in the JDK for a given list of classes.

By default `WhatsNewInJava` lists new methods added in the 9, 10 and 11 JDK releases.

This program needs the JDK sources to work:

```console
sudo apt-get install openjdk-11-source
cd /usr/lib/jvm/openjdk-11
sudo unzip src.zip
```
It uses Java `Stream` functionality, illustrates `Spliterator` usage and makes use of some of Java 11 features.

Written with Java 11, JBang and Picocli.

```console
$ jbang whats-new-in-java@grumpyf0x48 --help
Usage: WhatsNewInJava [-achvV] [-m=<module>] [-s=release]... <sourcesPath>
                      <classNames>...
Display methods added to a Java class in a given JDK release
      <sourcesPath>        JDK sources path
      <classNames>...      Class names or regexps
  -a, --not-modified-classes
                           Show all classes even not modified ones (default:
                             false)
  -c, --only-class-names   Show only names of modified classes, not their
                             methods (default: false)
  -h, --help               Show this help message and exit.
  -m, --module=<module>    Module (java.base, java.desktop, java.logging ...)
                             where to search classes (default: java.base)
  -s, --since=release      JDK release (1.8, 9, 10, 11 ...) (default: 9, 10, 11)
  -v, --verbose            Activate verbose mode (default: false)
  -V, --version            Print version information and exit.
```

### Sample use

Lists the changes made for `Stream` feature in Java 8 for `Iterable`, `Collection` and `List` classes:

```console
$ jbang whats-new-in-java@grumpyf0x48 --since 1.8 /usr/lib/jvm/openjdk-11 java.lang.Iterable java.util.Collection java.util.List
public interface Iterable<T> // since 1.5
{
    default void forEach(Consumer<? super T> action); // since 1.8
    default Spliterator<T> spliterator(); // since 1.8
}

public interface Collection<E> extends Iterable<E> // since 1.2
{
    default boolean removeIf(Predicate<? super E> filter); // since 1.8
    default Spliterator<E> spliterator(); // since 1.8
    default Stream<E> stream(); // since 1.8
    default Stream<E> parallelStream(); // since 1.8
}

public interface List<E> extends Collection<E> // since 1.2
{
    default void replaceAll(UnaryOperator<E> operator); // since 1.8
    default void sort(Comparator<? super E> c); // since 1.8
    default Spliterator<E> spliterator(); // since 1.8
}
```

Lists `Optional` and `Stream` changes introduced in Java 9:

```console
$ jbang whats-new-in-java@grumpyf0x48 /usr/lib/jvm/openjdk-11 --since 9 java.util.Optional java.util.stream.Stream
public final class Optional<T> // since 1.8
{
    public void ifPresentOrElse(Consumer<? super T> action, Runnable emptyAction); // since 9
    public Optional<T> or(Supplier<? extends Optional<? extends T>> supplier); // since 9
    public Stream<T> stream(); // since 9
}

public interface Stream<T> extends BaseStream<T, Stream<T>> // since 1.8
{
    default Stream<T> takeWhile(Predicate<? super T> predicate); // since 9
    default Stream<T> dropWhile(Predicate<? super T> predicate); // since 9
    public static<T> Stream<T> ofNullable(T t); // since 9
    public static<T> Stream<T> iterate(T seed, Predicate<? super T> hasNext, UnaryOperator<T> next); // since 9
}
```
