# Immutability

Most objects in Mobius are required to be immutable. Immutability imposes a constraint on what your
objects can do, therefore making them easier to reason about. The Mobius framework is in fact based
on the principle that [Model](../reference-guide/model.md), [Event](../reference-guide/event.md),
and [Effect](../reference-guide/effect.md) objects are immutable – it wouldn’t be possible to have
Mobius without this constraint.

For an object to be immutable, it is not enough to make all member fields final. Any object referred
to must also be immutable. If any field references a mutable object, then your object is not
immutable (the exception being if the field isn’t important for behaviour; an example
is `java.util.String` instances that are immutable even though they cache the hash code in a mutable
field). In particular be careful with arrays - they might be marked as `final`, but that only makes
the reference to the array final, not the values inside the array.

It might be tempting sometimes to use e.g. a plain `List` instead of an `ImmutableList`, and you
might in fact get away with it, especially if it’s only your own code that touches the data. But
it’s strongly advised to avoid this, as it is a potential source of errors, and breaking the
immutability contract means you no longer get any guarantees from the framework. While it might not
seem like a big issue, it might have unintended consequences or cause problems later on.
