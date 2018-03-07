# SimpleChat

AIML is terribly verbose, and writing complex conversational structures
is a nightmare. Yes, you can do it - and it's really powerful - but
the XML nature of everything and the way topics work really burns my
brain. Chatscript is easier, much easier, but it's in C++. And my essential
usecase is Minecraft chatbots, so I can't use that. 
So this is a simplified (hah) thing. This documentation is really 
just for me, at the moment.

THIS IS VERY EARLY WORK. (See the [todo list](TODO.md))

## Bots
- The primary object is a `Bot`, which has a set of topics (see below)
- Each Bot can have multiple `BotInstance` objects, which are a single
speaker sharing the same chat data (but having some private, perhaps).
Thus you could have a whole lot of "soldier" or "shopkeeper" BotInstances,
all based on one Bot.
- The users are represented by `Source` objects, and each conversation
between a `BotInstance` and a `Source` has some private data, called
"conversation data".

## Creating a bot
Typically goes like this:
```java
Bot b = new Bot(Paths.get("/some/directory/or/other");
instance = new BotInstance(b);
source = new Source();
```
The `Source` object is something to associate a conversation with.
It will typically be embedded in some entity in your code. It has
a `receive` method to override if you wish, but typically a conversation
is done by repeatedly calling
```java
String reply = instance.handle(string,source);
```

## Bot directory
The bot directory should contain
- `config.conf` file listing the topics, substitutions etc.
- `.sub` files with substitutions
- `.topic` files each containing a topic

## The configuration file
The config file must be called `config.conf`. It contains the following:
- `topics` entries each giving a list of topics, each of which is loaded
from a `.topic` file. A topic is a set of pattern/action pairs: when a
pattern is matched, the action fires and pattern matching stops.
- `subs` entries each giving the name of a substitution set, which is loaded
from a `.sub` file
- an optional `init` entry followed by a block of Action language (see below)
which will set up initial values for conversation variables and maybe do
some other things. A `#` starts a comment.

Here is an example:
```
# This is a test bot!

# here are some substitution files.

subs "subs1.sub"
subs "subs2.sub"

# primary topics, which can be rearranged in priority from within
# action code.

topics {main cats dogs}

# topics in different lists can be promoted and demoted but not
# outside their list, so these will always run after the topics
# above. The last topic list is generally for "catch-all" patterns.

topics {bottom}

# and here's an init block which just sets the instance variable
# `foo` to zero.
init
    0 int !@foo
;
```

## Regex substitutions
Each bot can have a file (or set of files) containing regex substitutions
associated with it. These will be processed before any other input,
and are always processed. They are typically used to substitute
things like "I'm" and "I am" with "IAM" to make parsing easier.
Multiple bots can share substitution sets.

A substitution file is appended to a bot's substitutions by using a line
of the form
```
subs <subfilename>
```
in the config file. The file path is relative to the bot directory.

The format for the files is
lines consisting of a regex and a replacement string, separated by default
by a colon. Two directives exist, which should be on their own lines.
The `#include` directive has a file argument and will include a file
of substitutions. The `#sep` directive has a string (actually regex)
argument and changes the separator for this file. The argument is separated
by a space. All other `#` lines are comments.
A (very brief) example:
```
[iI]'m:Iam
[Ii]\s+am:Iam
[yY]ou\s+are:youre
[yY]ou're:youre
include more.subst
```


### Initial action
This is written in the action language (see below and 
[here](ACTIONS.adoc)) and runs when an instance of this bot
is created, but just throws away the output. It is typically
used to initialise instance variables. Setting a conversation
variable will cause a runtime error, because the bot isn't in
a conversation.

### Topics
Topics are (loosely speaking) subjects of conversation.
Each topic consists of a list of pattern/action pairs, which
are run through in order when the user provides input.
When a pattern matches, the action runs and produces some
output which is passed to the user (as well as perhaps doing other
things). All processing then stops.
More specific patterns should therefore be at the top of the topic file,
so they get a chance to match first.

Sometimes a special "pseudotopic" can be in play, such as when
the `next` command is used in action code to specify a set
of patterns to try to match with the next input. This is done
to produce dialogue tree effects. In this case, the pseudotopic
will try to match its patterns before any real topics.

Topics are arranged into lists. Within each list, topics can
be promoted or demoted to the top and bottom of the list by
actions. There can be any number of lists, but the example config
above is a typical case, using only two: a main list for all
the general conversational topics, and a bottom list for catch-all
phrases. The topics are processed within their list, and their
lists are processed in order. This is so that you can (say) demote
a topic, but have it still try to match its patterns before any
catch-all patterns try.

The `topics` command in the config file specifies a new topic
list. Following it, in curly braces, are the topic names. These
are loaded from `.topic` files in the same directory as the bot,
so the line
`topics {main}` will load the `main.topic` file.

Here is an example topic file:
```
# this is a named pattern/action pair. The string is the the pattern,
# the bit between it and the semicolon is the action. This one stacks
# the output "Hi, how are you?", and then sets up a subpattern tree
# and tells the system to use it to parse responses to this output.

+hellopattern "([hello hi] .*)"
    "hi how are you?"
    {
        # each subpattern is a pattern/action pair.
        # the pattern is this bit. It matches:
        # - possibly "Iam" (substituted for "I am" or "I'm")
        # - then either good, fine or well
        # - then everything else.

        "(?Iam [good fine well] .*)"

            # and this is the action, which just stacks an output

            "Glad to hear it.";

        # This pattern matches
        # - "Iam" optionally
        # - then "bad" or the sequence "not too"
        # - then everything else
        
        "(?Iam [bad (not too)] .*)"
            "Oh, I'm sorry";
    }
    # "next" tells the system to try to match from the subpattern list
    # we have just put on the stack, the next time we get input.
    next; 
    
# this anonymous pattern catches everything, and runs when nothing
# else in the topic has matched. It captures the input as "$foo"
# and this gets used to generate the output. You'd normally
# put this in a topic in the bottom topic list.

+"$foo=.*"
    "I don't know how to respond to " $foo +;
```
Note that each pair is preceded by `+` and an optional name, followed
by the pattern string in quotes, followed by the actions and a semicolon.
The pattern name can be used to disable and enable a pattern in a topic
from inside an action.

Whole topics can also be enabled and disabled, as well as being 
promoted and demoted to the top or bottom of their list.

## Patterns
For matching, the input is lower-cased, all punctuation is removed
and finally it is split into words. Pattern matching is done per-word.
The entire pattern must be in a pair of quotes. Most patterns
will be sequences, so you'll see a lot of `"(...)"`.

### pattern elements
- plain words match themselves
- `^` negates the next pattern
- `[..]` matches any of the included patterns
- `(..)` matches all the included patterns in sequence
- `..*` matches anything (including nothing) until the previous pattern has a match;
it always succeeds
- `?..` matches the next pattern, but carries on if it fails
- `..+` matches at least one token until the previous pattern has a match;
- `^` negates the following pattern, but does not consume - it should be followed by what you want in that place.
A common pattern might be `^cat .` which will match "not a cat"

### Star gotchas
A pattern like `(bar foo)+ bar` may cause problems, because when presented
with a string like "bar foo bar" immediately match the end token (bar)
and so fail. Make sure your end pattern is not the start of a star sequence
pattern. I'm sure there's a clever way around this.

### Other gotchas
Negate nodes are "fun".

### Labels
Putting `$labelname=` before a pattern element marks it so that
the data it matches will be stored in a variable. In the case of '*' and
'+', the variable `$labelname_ct` is set to the match count.


### Examples
```
$all=(hello $name=.+)
(foo* hello $name=.*)
```



# Actions
These are in the form of a sequence of instructions in an RPN language,
which should always leave a string on the stack. They are always terminated
by a semicolon. The simplest is just a string:
```
+([hello hi] $name=.*)
"Hi, how are you?";
```
One special and complex instruction is an entire set of subpatterns and
actions. When these are set using the `next` command, the conversation will
try these patterns first. They are pattern/action pairs as normal, but
defined in curly brackets:
```
+pat "([hello hi] .*)"
    "hi how are you?"
    {
        "([good fine well] .*)"
            "Glad to hear it.";
        "([bad (not too)] .*)"
            "Oh, I'm sorry";
    }
```
More details on the action language [here](ACTIONS.adoc).
