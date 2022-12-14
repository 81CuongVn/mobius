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
package com.spotify.mobius.test;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;

import com.spotify.mobius.Next;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeDiagnosingMatcher;

/** Provides utility functions for matching {@link Next} instances in tests. */
public final class NextMatchers {
  private NextMatchers() {
    // prevent instantiation
  }

  /**
   * Returns a matcher that matches {@link Next} instances without a model.
   *
   * @param <M> the model type
   * @param <F> the effect type
   */
  public static <M, F> Matcher<Next<M, F>> hasNoModel() {
    return new TypeSafeDiagnosingMatcher<Next<M, F>>() {
      @Override
      protected boolean matchesSafely(Next<M, F> item, Description mismatchDescription) {
        if (item.hasModel()) {
          mismatchDescription.appendText("it had a model: " + item.modelUnsafe());
          return false;
        }

        return true;
      }

      @Override
      public void describeTo(Description description) {
        description.appendText("Next without model");
      }
    };
  }

  /**
   * Returns a matcher that matches {@link Next} instances with a model.
   *
   * @param <M> the model type
   * @param <F> the effect type
   */
  public static <M, F> Matcher<Next<M, F>> hasModel() {
    return new TypeSafeDiagnosingMatcher<Next<M, F>>() {
      @Override
      protected boolean matchesSafely(Next<M, F> item, Description mismatchDescription) {
        if (!item.hasModel()) {
          mismatchDescription.appendText("it had no model");
          return false;
        }

        return true;
      }

      @Override
      public void describeTo(Description description) {
        description.appendText("Next with any model");
      }
    };
  }

  /**
   * Returns a matcher that matches {@link Next} instances with a model that is equal to the
   * supplied one.
   *
   * @param expected the expected model
   * @param <M> the model type
   * @param <F> the effect type
   */
  public static <M, F> Matcher<Next<M, F>> hasModel(M expected) {
    return hasModel(equalTo(expected));
  }

  /**
   * Returns a matcher that matches {@link Next} instances with a model that matches the supplied
   * model matcher.
   *
   * @param matcher the matcher to apply to the model
   * @param <M> the model type
   * @param <F> the effect type
   */
  public static <M, F> Matcher<Next<M, F>> hasModel(Matcher<M> matcher) {
    return new TypeSafeDiagnosingMatcher<Next<M, F>>() {
      @Override
      protected boolean matchesSafely(Next<M, F> item, Description mismatchDescription) {
        if (!item.hasModel()) {
          mismatchDescription.appendText("it had no model");
          return false;
        }

        if (!matcher.matches(item.modelUnsafe())) {
          mismatchDescription.appendText("the model ");
          matcher.describeMismatch(item.modelUnsafe(), mismatchDescription);
          return false;
        }

        return true;
      }

      @Override
      public void describeTo(Description description) {
        description.appendText("Next with model ").appendDescriptionOf(matcher);
      }
    };
  }

  /**
   * Returns a matcher that matches {@link Next} instances with no effects.
   *
   * @param <M> the model type
   * @param <F> the effect type
   */
  public static <M, F> Matcher<Next<M, F>> hasNoEffects() {
    return new TypeSafeDiagnosingMatcher<Next<M, F>>() {
      @Override
      protected boolean matchesSafely(Next<M, F> item, Description mismatchDescription) {
        if (item.hasEffects()) {
          mismatchDescription.appendText("it had effects: " + item.effects());
          return false;
        }

        return true;
      }

      @Override
      public void describeTo(Description description) {
        description.appendText("Next without effects");
      }
    };
  }

  /**
   * Returns a matcher that matches {@link Next} instances whose effects match the supplied effect
   * matcher.
   *
   * @param matcher the matcher to apply to the effects
   * @param <M> the model type
   * @param <F> the effect type
   */
  public static <M, F> Matcher<Next<M, F>> hasEffects(Matcher<Iterable<F>> matcher) {
    return new TypeSafeDiagnosingMatcher<Next<M, F>>() {
      @Override
      protected boolean matchesSafely(Next<M, F> item, Description mismatchDescription) {
        if (!item.hasEffects()) {
          mismatchDescription.appendText("it had no effects");
          return false;
        }

        if (!matcher.matches(item.effects())) {
          mismatchDescription.appendText("the effects were ");
          matcher.describeMismatch(item.effects(), mismatchDescription);
          return false;
        }
        return true;
      }

      @Override
      public void describeTo(Description description) {
        description.appendText("Next with effects ").appendDescriptionOf(matcher);
      }
    };
  }

  /**
   * Returns a matcher that matches if all the supplied effects are present in the supplied {@link
   * Next}, in any order. The {@link Next} may have more effects than the ones included.
   *
   * @param effects the effects to match (possibly empty)
   * @param <M> the model type
   * @param <F> the effect type
   * @return a matcher that matches {@link Next} instances that include all the supplied effects
   */
  @SafeVarargs
  public static <M, F> Matcher<Next<M, F>> hasEffects(F... effects) {
    return hasEffects(hasItems(effects));
  }

  /**
   * Returns a matcher that matches {@link Next} instances with no model and no effects.
   *
   * @param <M> the model type
   * @param <F> the effect type
   */
  public static <M, F> Matcher<Next<M, F>> hasNothing() {
    return allOf(hasNoModel(), hasNoEffects());
  }
}
