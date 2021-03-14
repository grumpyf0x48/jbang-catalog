# jbang-catalog

My collection of Jbang scripts

## Ssh

A basic SSH client.

Written with Apache Mina, Jbang and Picocli.

### Usage

```console
$ jbang ssh@grumpyf0x48 --help
Usage: Ssh [-hV] [-a=<authenticationTimeout>] [-c=<connectionTimeout>]
           [-i=<identityFile>] [-p=<password>] [--port=<port>] <destination>
           [<command>...]
Ssh client written with Apache Mina, Jbang and Picocli
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

Written with Jbang and Picocli.

### Usage

```console
$ jbang sort@grumpyf0x48 --help
Usage: Sort [-himnrV] [<elements>...]
Sort made with jbang
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

Written with Jsoup, Gson, Jbang and Picocli.

### Usage

```console
$ jbang links@grumpyf0x48 --help
Usage: Links [-hvV] [-a=<archivedSiteUrl>] [-f=<firstPage>] [-l=<lastPage>]
             [-r=<rootUrl>] <file>
Links made with jbang
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

Written with Gson, Jbang and Picocli.

### Usage

```console
$ jbang html@grumpyf0x48 --help
Usage: Html [-hV] -i=<inputFile> -o=<outputFile>
Html made with Gson, Jbang and Picocli
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

Written with OpenJDK jol, Jbang and Picocli.

### Usage

```console
$ jbang sizeof@grumpyf0x48 --help
Usage: SizeOf [-hV] [<types>...]
SizeOf made with jbang and OpenJDK jol
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

Written with jGit, Jbang and Picocli.

### Usage

```console
$ jbang git-clone@grumpyf0X48 --help
Usage: GitClone [-hnV] [--bare] [-b=<branch>] [-i=<identityFile>] <repository>
                [<directory>]
GitClone made with jbang
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

It is like `git clone` but restricted to some files or directories of the repository.

### Usage

```console
$ jbang git-get@grumpyf0x48 --help
Usage: GitGet [-hV] [--fresh] [-b=<branch>] [-i=<identityFile>] <repository>
              <directory> <paths>...
GitGet made with jbang
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
