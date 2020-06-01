/*
 * -\-\-
 * Mobius
 * --
 * Copyright (c) 2017-2018 Spotify AB
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
package com.spotify.mobius2.android;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.Lifecycle;
import com.spotify.mobius2.First;
import com.spotify.mobius2.Mobius;
import com.spotify.mobius2.Next;
import com.spotify.mobius2.Update;
import com.spotify.mobius2.functions.Consumer;
import com.spotify.mobius2.runners.ImmediateWorkRunner;
import java.util.LinkedList;
import java.util.List;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class MobiusLoopViewModelTest {

  @Rule public InstantTaskExecutorRule rule = new InstantTaskExecutorRule();
  private List<MobiusLoopViewModelTestUtilClasses.TestEvent> recordedEvents = new LinkedList<>();
  private final Update<
          MobiusLoopViewModelTestUtilClasses.TestModel,
          MobiusLoopViewModelTestUtilClasses.TestEvent,
          MobiusLoopViewModelTestUtilClasses.TestEffect>
      updateFunction =
          (model, event) -> {
            recordedEvents.add(event);
            return Next.noChange();
          };
  private MobiusLoopViewModel<
          MobiusLoopViewModelTestUtilClasses.TestModel,
          MobiusLoopViewModelTestUtilClasses.TestEvent,
          MobiusLoopViewModelTestUtilClasses.TestEffect,
          MobiusLoopViewModelTestUtilClasses.TestViewEffect>
      underTest;
  private MobiusLoopViewModelTestUtilClasses.TestViewEffectHandler<
          MobiusLoopViewModelTestUtilClasses.TestEvent,
          MobiusLoopViewModelTestUtilClasses.TestEffect,
          MobiusLoopViewModelTestUtilClasses.TestViewEffect>
      testViewEffectHandler;

  private FakeLifecycleOwner fakeLifecycle;
  private RecordingObserver<MobiusLoopViewModelTestUtilClasses.TestModel> recordingModelObserver =
      new RecordingObserver<>();
  private RecordingObserver<MobiusLoopViewModelTestUtilClasses.TestViewEffect>
      recordingForegroundViewEffectObserver = new RecordingObserver<>();
  private RecordingObserver<Iterable<MobiusLoopViewModelTestUtilClasses.TestViewEffect>>
      recordingBackgroundEffectObserver = new RecordingObserver<>();
  private MobiusLoopViewModelTestUtilClasses.TestModel initialModel;

  @Before
  public void setUp() {
    fakeLifecycle = new FakeLifecycleOwner();
    recordedEvents = new LinkedList<>();
    testViewEffectHandler = null;
    recordingModelObserver = new RecordingObserver<>();
    recordingForegroundViewEffectObserver = new RecordingObserver<>();
    recordingBackgroundEffectObserver = new RecordingObserver<>();
    initialModel = new MobiusLoopViewModelTestUtilClasses.TestModel("initial model");
    //noinspection Convert2MethodRef
    underTest =
        new MobiusLoopViewModel<>(
            (Consumer<MobiusLoopViewModelTestUtilClasses.TestViewEffect> consumer) -> {
              testViewEffectHandler =
                  new MobiusLoopViewModelTestUtilClasses.TestViewEffectHandler<>(consumer);
              return Mobius.loop(updateFunction, testViewEffectHandler)
                  .eventRunner(ImmediateWorkRunner::new)
                  .effectRunner(ImmediateWorkRunner::new);
            },
            initialModel,
            (MobiusLoopViewModelTestUtilClasses.TestModel model) -> First.first(model),
            new ImmediateWorkRunner(),
            100);
    underTest.getModels().observe(fakeLifecycle, recordingModelObserver);
    underTest
        .getViewEffects()
        .setObserver(
            fakeLifecycle,
            recordingForegroundViewEffectObserver,
            recordingBackgroundEffectObserver);
  }

  @Test
  public void testViewModelgetModelAtStartIsInitialModel() {
    fakeLifecycle.handleLifecycleEvent(Lifecycle.Event.ON_RESUME);

    assertThat(underTest.getModel().name, equalTo("initial model"));
    recordingModelObserver.assertValues(initialModel);
  }

  @Test
  public void testViewModelSendsEffectsIntoLoop() {
    fakeLifecycle.handleLifecycleEvent(Lifecycle.Event.ON_RESUME);
    underTest.dispatchEvent(new MobiusLoopViewModelTestUtilClasses.TestEvent("testable"));
    assertThat(recordedEvents.size(), equalTo(1));
    assertThat(recordedEvents.get(0).name, equalTo("testable"));
  }

  @Test
  public void testViewModelDoesNotSendViewEffectsIfLifecycleIsPaused() {
    fakeLifecycle.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE);
    testViewEffectHandler.viewEffectConsumer.accept(
        new MobiusLoopViewModelTestUtilClasses.TestViewEffect("view effect 1"));
    assertThat(recordingForegroundViewEffectObserver.valueCount(), equalTo(0));
    assertThat(recordingBackgroundEffectObserver.valueCount(), equalTo(0));
  }

  @Test
  public void
      testViewModelSendsViewEffectsToBackgroundObserverWhenLifecycleWasPausedThenIsResumed() {
    fakeLifecycle.handleLifecycleEvent(Lifecycle.Event.ON_RESUME);
    fakeLifecycle.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE);
    testViewEffectHandler.viewEffectConsumer.accept(
        new MobiusLoopViewModelTestUtilClasses.TestViewEffect("view effect 1"));
    assertThat(recordingBackgroundEffectObserver.valueCount(), equalTo(0));
    fakeLifecycle.handleLifecycleEvent(Lifecycle.Event.ON_RESUME);
    assertThat(recordingBackgroundEffectObserver.valueCount(), equalTo(1));
  }

  @Test
  public void testViewModelSendsViewEffectsToForegroundObserverWhenLifecycleIsResumed() {
    fakeLifecycle.handleLifecycleEvent(Lifecycle.Event.ON_RESUME);
    testViewEffectHandler.viewEffectConsumer.accept(
        new MobiusLoopViewModelTestUtilClasses.TestViewEffect("view effect 1"));
    assertThat(recordingForegroundViewEffectObserver.valueCount(), equalTo(1));
    assertThat(recordingBackgroundEffectObserver.valueCount(), equalTo(0));
  }

  @Test
  public void testViewModelDoesNotTryToForwardEventsIntoLoopAfterCleared() {
    fakeLifecycle.handleLifecycleEvent(Lifecycle.Event.ON_RESUME);
    underTest.onCleared();
    underTest.dispatchEvent(new MobiusLoopViewModelTestUtilClasses.TestEvent("don't record me"));
    assertThat(recordedEvents.size(), equalTo(0));
  }
}