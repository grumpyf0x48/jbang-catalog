# jbang-catalog

My collection of Jbang scripts

## Ssh

A basic SSH client written with Apache Mina, Jbang and Picocli.

```console
$ jbang ssh@grumpyf0x48 -i ~/.ssh/id_rsa pi@192.168.0.108 "free -h"
```

## Sort

```console
$ jbang sort@grumpyf0x48 -r -m $(exa -l ~/Downloads | cut -d " " -f 2 | xargs)
```

A sample sort program to illustrate Java stream and Array sort use written with Jbang and Picocli.
