# Action language
The action language is based on Angort, because it was pretty easy to write
a quick-and-dirty parser and interpreter for such a language. As such, it's
a Forth-based RPN stack language.

## Basic operations
All operations take arguments on the stack, which come before them in the language.
For example, a pattern/action pair to read two numbers and add them together might look like this:

```
+"(add $foo=. $bar=.)"
    $foo double # stack the first argument and convert to double
    $bar double # stack the second argument and convert to double
    + str       # add together the two top stack values and convert to string
;
```
A quoted string or number compile to instructions which stack them as strings, double-precision
floating point (referred to as "doubles" hereafter) or integers.
Integers are stacked if the number is equal to its floor, so `2.0` will be stacked as the
integer 2. This is due to how the tokeniser works, sorry.

### Maths and comparison operators
Binary operations have the stack picture `(a b -- c)`: that is, they remove two items
from the stack and replace them with a single item.
- `+` adds the values; if either is a string, will convert both to strings and concatenate them
- `-` subtracts `b` from `a`; one must be a number, other will be converted if a string
- `/` divides `b` by `a`: one must be a number, other will be converted if a string
- `%` finds `a` mod `b`; one must be a number, other will be converted if a string
- `=` pushes 1 if the values are the same type and equal, 0 otherwise
- `!=` pushes 0 if the values are the same type and equal, 1 otherwise
- `<` pushes 1 if `a` < `b`, else pushes 0
- `<=` pushes 1 if `a` <= `b`, else pushes 0
- `>` pushes 1 if `a` > `b`, else pushes 0
- `>=` pushes 1 if `a` >= `b`, else pushes 0

Unary operations have the stack picture `(a -- b)`: they replace a value with a modified value.
The core operations supported are:
- `not` replaces a nonzero value with zero, and vice versa
- `neg` replaces the value with its negative (having converted it to a number)

### Conversion functions
These all have the picture `(a -- b)`, like unary operators, and convert between types.
- `double` converts to a string
- `int` converts to an integer
- `str` converts to a string

### String functions
- `trim` `(a -- b)` trims leading and trailing whitespace from a string

### Stack manipulation
- `dup` `(a -- a a)` duplicates the item on the stack

### Debugging
- `dp` `(a --)` prints a value to the system logger

### Variable access
There are three sets of variables
- *instance* variables are private to each `BotInstance`, each communicating entity.
- *conversation* variables are private to each `BotInstance/Source` pair, that is,
each conversation between a bot and a user.
- *pattern* variables are those matched in a pattern, such as `foo` and `bar` in the example
above.

The different sorts of variables have different sigils in front of their name:
- conversation variables have no sigil, they are the default
- instance variables have the `@` sigil
- pattern variables have the `$` sigil

#### Fetching variables
- `!varname` will pop the stack and store into the conversation variable `varname`. 
- `?varname` will push the value of conversation variable `varname`, or the string `"??"` if 
it has not been set.
- `?@varname` will pop the stack and store into the instance  variable `varname`. 
- `?@varname` will push the value of instance variable `varname`, or the string `"??"` if 
it has not been set.
- `$varname` will push the value of pattern variable `varname`, or `"??"` if not set - note
that there is no `?` here, because you cannot set a pattern variable.

## Pattern manipulation
- `recurse` `(s --)` feeds the string back into the conversation system as if it were
spoken to the bot, and stacks the result. Take care you don't recurse infinitely!
- `next` `(p --)` specifies the subpattern block to use for preferential matching of the next input.

## Flow control

### `if .. then .. else`
This is the basic flow control statement. It might seem a bit odd if you're not used to
languages of the Forth family, but it works like this:
```
<code that leaves integer on stack>
if
<part that runs if integer was true>
then
```
or
```
<code that leaves integer on stack>
if
<part that runs if integer was true>
then
<part that runs if integer was false>
else
```
For example
```
?@foo 5 = if "Five!" else "Not five!" then
```
will check if instance variable `foo` is 5. If it is, it will stack "Five!", otherwise
it will stack "Not five!" These statements can be nested.

### `cases`: or, how to do `else if`
We don't have `else if` in this language because of the way it's parsed (how would you separate
the condition part from the action part of the previous `if`?). Instead, the `cases` construction
serves the same role. It has the form
```
cases
    <condition> if <action> case
    <condition> if <action> case
    ...
    <action> otherwise
```
Here's an example which converts the string obtained from the pattern, `$n`, into an integer conversation variable for testing.
```        
    +"(case test $n=.)"
    $n int !n
    ?n 0 = if "Zero" case
    ?n 1 = if "One" case
    ?n 2 = if "Two" case
    ?n 10 < if "Between three and nine" case
    "Something else" otherwise;
```
    

### "Infinite" loops
The words `loop` and `endloop` enclose a loop, which is notionally infinite.
The `leave` and `ifleave` words leave the enclosing loop. `ifleave` pops an integer
from the stack and leaves the loop if it is non-zero; as such it is equivalent to
`if leave then`. Loops may be nested.
This example counts to the number user specifies:
```
    +"(count to $n=.*)"
    0!ct
    ""
    loop
        ?ct 1+ !ct
        ?ct + " " +
        ?ct $n int = ifleave
    endloop trim;
```

### Early exit 
We can exit from an action early using the `stop` word. Note that we must still
leave a string on the stack for the action to sent to the user. This word will
work inside loops and other control structures.