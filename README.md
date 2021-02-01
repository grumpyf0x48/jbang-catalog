# jbang-catalog

My collection of Jbang scripts

## Ssh

A basic SSH client written with Apache Mina, Jbang and Picocli.

```console
$ jbang ssh@grumpyf0x48 -i ~/.ssh/id_rsa pi@192.168.0.108 "free -h"
```

## Sort

A sample sort program to illustrate Java stream and Array sort use written with Jbang and Picocli.

```console
$ jbang sort@grumpyf0x48 -r -m $(exa -l ~/Downloads | cut -d " " -f 2 | xargs)
```

## Links

A program to retrieve the posts metadata of my former blog archived on https://web.archive.org written with Jsoup, Jbang and Picocli.

```console
$ jbang links@grumpyf0x48 -v -f 1 -l 4 -o firstPosts.json
```
