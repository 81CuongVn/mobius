/*
 * -\-\-
 * Mobius
 * --
 * Copyright (c) 2017-2020 Spotify AB
 * --
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * -/-/-
 */
package com.spotify.mobius;

import static com.spotify.mobius.internal_util.Preconditions.checkNotNull;

import com.spotify.mobius.functions.Consumer;
import com.spotify.mobius.runners.WorkRunner;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

class MobiusLoopController<M, E, F>
    implements MobiusLoop.Controller<M, E>, ControllerActions<M, E> {

  private final MobiusLoop.Factory<M, E, F> loopFactory;
  private final M defaultModel;
  @Nullable private final Init<M, F> init;
  private final WorkRunner mainThreadRunner;

  private ControllerStateBase<M, E> currentState;

  MobiusLoopController(
      MobiusLoop.Factory<M, E, F> loopFactory,
      M defaultModel,
      @Nullable Init<M, F> init,
      WorkRunner mainThreadRunner) {

    this.loopFactory = checkNotNull(loopFactory);
    this.defaultModel = checkNotNull(defaultModel);
    this.init = maybeWrapInit(init, loopFactory);
    this.mainThreadRunner = checkNotNull(mainThreadRunner);
    goToStateInit(defaultModel);
  }

  private Init<M, F> maybeWrapInit(
      @Nullable Init<M, F> delegate, MobiusLoop.Factory<M, E, F> loopFactory) {
    if (delegate == null) {
      return null;
    }

    // In this configuration, the controller has an `Init` function, which it will invoke when
    // starting the loop (this means the loop cannot also have an `Init`; or an exception will be
    // thrown). We want to ensure that the same Mobius.Logger is used for logging before and after
    // Init as the rest of the loop events. The APIs don't allow us to do that in a nice way, so
    // this casting is a hack around that issue. In practice, the below instanceof check will never
    // fail.
    if (!(loopFactory instanceof Mobius.Builder)) {
      return delegate;
    }

    Mobius.Builder<M, E, F> builder = (Mobius.Builder<M, E, F>) loopFactory;

    return new LoggingInit<>(delegate, builder.getLogger());
  }

  @Override
  public synchronized boolean isRunning() {
    return currentState.isRunning();
  }

  // note on synchronization: since this method should never change the controller state, only that
  // of the controlled loop, which has its own concurrency protection, it doesn't need to be
  // synchronized.
  private void dispatchEvent(E event) {
    currentState.onDispatchEvent(event);
  }

  // note on synchronization: since this method should never change the controller state, and will
  // be called from the `mainThreadRunner` only, it doesn't need to be synchronized.
  private void updateView(M model) {
    currentState.onUpdateView(model);
  }

  @Override
  public synchronized void connect(Connectable<M, E> view) {
    currentState.onConnect(checkNotNull(view));
  }

  @Override
  public synchronized void disconnect() {
    currentState.onDisconnect();
  }

  @Override
  public synchronized void start() {
    currentState.onStart();
  }

  @Override
  public synchronized void stop() {
    currentState.onStop();
  }

  @Override
  public synchronized void replaceModel(M model) {
    checkNotNull(model);
    currentState.onReplaceModel(model);
  }

  @Override
  @Nonnull
  public synchronized M getModel() {
    return currentState.onGetModel();
  }

  @Override
  public void postUpdateView(final M model) {
    mainThreadRunner.post(
        new Runnable() {
          @Override
          public void run() {
            updateView(model);
          }
        });
  }

  @Override
  public synchronized void goToStateInit(M nextModelToStartFrom) {
    currentState = new ControllerStateInit<>(this, nextModelToStartFrom);
  }

  @Override
  public synchronized void goToStateCreated(
      Connection<M> renderer, @Nullable M nextModelToStartFrom) {

    if (nextModelToStartFrom == null) {
      nextModelToStartFrom = defaultModel;
    }

    currentState = new ControllerStateCreated<M, E, F>(this, renderer, nextModelToStartFrom);
  }

  @Override
  public void goToStateCreated(Connectable<M, E> view, M nextModelToStartFrom) {

    DiscardAfterDisposeConnectable<M, E> safeModelHandler =
        new DiscardAfterDisposeConnectable<>(checkNotNull(view));

    Connection<M> modelConnection =
        safeModelHandler.connect(
            new Consumer<E>() {
              @Override
              public void accept(E event) {
                dispatchEvent(event);
              }
            });

    goToStateCreated(checkNotNull(modelConnection), nextModelToStartFrom);
  }

  @Override
  public synchronized void goToStateRunning(Connection<M> renderer, M nextModelToStartFrom) {
    ControllerStateRunning<M, E, F> stateRunning =
        new ControllerStateRunning<>(this, renderer, loopFactory, nextModelToStartFrom, init);

    currentState = stateRunning;

    stateRunning.start();
  }
}
