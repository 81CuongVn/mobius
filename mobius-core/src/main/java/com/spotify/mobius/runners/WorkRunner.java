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
package com.spotify.mobius.runners;

import com.spotify.mobius.disposables.Disposable;

/** Interface for posting runnables to be executed using different scheduling mechanisms. */
public interface WorkRunner extends Disposable {
  /**
   * Must discard any new {@link Runnable} immediately after dispose method of {@link Disposable} is
   * called. Not doing this may result in undesired side effects, crashes, race conditions etc.
   */
  void post(Runnable runnable);
}
