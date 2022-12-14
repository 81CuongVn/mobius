# Logging and Error Handling

## Logging

In addition to the business-logic level logging capabilities that you can get from configuring
a `MobiusLoop` instance with a `MobiusLoop.Logger`, Mobius also uses [SLF4J](http://slf4j.org/) for
internal logging. These messages - especially at ERROR and WARN level - can sometimes be useful to
get access to. For that, you'll need to set up a logging framework for your application as described
in the [SLF4J documentation](http://slf4j.org/manual.html#swapping). We don't use an open source
logging framework at Spotify, so we do not have any recommendation for what to use for Android, but
there seem to be a few options.

## Error Handling

Mobius generally tries to expose programmer errors (normally manifesting as `RuntimeException`s) by
simply crashing. When using `WorkRunner`s, crashing isn't really possible, because what actually
happens to an uncaught exception depends on the thread's `UncaughtExceptionHandler`. Mobius's
default behaviour in that situation is to log the exception at ERROR level, and then ignore it. If
you wish to do something else - like crash the entire application or report the exception to some
other system - then you can do so through configuring an error handler
via `com.spotify.mobius.MobiusHooks.setErrorHandler`.