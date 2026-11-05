# Third-party notices & attributions

SingeeIche bundles or depends on third-party software and data. This file collects
the required attributions and license notices. The SmartTube/NewTube-derived code
is MIT-licensed (see [`LICENSE`](LICENSE)).

## Community data services

NewTube can query the following community services. Their **data** is licensed
separately from their code, and NewTube uses it under those terms:

- **SponsorBlock** — sponsor-segment data © SponsorBlock and its contributors,
  licensed under **CC BY-NC-SA 4.0**
  (https://creativecommons.org/licenses/by-nc-sa/4.0/).
  API: https://sponsor.ajay.app · Project: https://github.com/ajayyy/SponsorBlock
  NewTube uses this data unmodified to skip segments; it does not redistribute
  the database. NewTube is a non-commercial, free, open-source project.
- **DeArrow** — title/thumbnail branding data © DeArrow and its contributors,
  licensed under **CC BY-NC-SA 4.0**.
  API: https://sponsor.ajay.app/api/branding · Project: https://dearrow.ajay.app
  Used unmodified and non-commercially.
- **Return YouTube Dislike** — dislike-count data via
  https://returnyoutubedislikeapi.com (https://returnyoutubedislike.com).
  Used read-only, per the project's public API.

> These services are operated by their respective projects and are **not**
> affiliated with NewTube. NewTube's non-commercial status is a condition of
> using the SponsorBlock/DeArrow data (the NC term); do not add advertising,
> paid tiers, or other commercial monetization without re-evaluating these
> licenses.

## Bundled / vendored code

- **DoubleTapPlayerView** (`doubletapplayerview-media3/`) — © Vincent Kammerer,
  **MIT** (https://github.com/vkay94/DoubleTapPlayerView). NewTube vendors a
  media3-rebased fork. See [`doubletapplayerview-media3/LICENSE`](doubletapplayerview-media3/LICENSE).
- **Slidr / SlidableActivity** (`slidableactivity/`) — © r0adkll, **Apache-2.0**.
  See [`slidableactivity/LICENSE.md`](slidableactivity/LICENSE.md).
- **filepicker-lib** (`filepicker-lib/`) — **Apache-2.0**.
  See [`filepicker-lib/LICENSE`](filepicker-lib/LICENSE).
- **PeerTube browser** — instance search and API models were adapted from the
  user's local **PeerIche** project. The user authorized this reuse; PeerIche is
  authored by Arctic Fox Apps. Its third-party components remain under their
  original licenses.
- **PeerTube URL validation and resolver conventions** — adapted from the user's
  local **RussTube** project, © 2026 Evgeny Stepanischev, **MIT**. The original
  license terms are reproduced below.

### RussTube MIT license

```text
MIT License

Copyright (c) 2026 Evgeny Stepanischev

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

## Submodules

- **MediaServiceCore** and **SharedModules** — © yuliskov, **MIT** (same license
  as SmartTube). NewTube uses its own forks of these repositories.
  - SharedModules bundles **j2v8** (© EclipseSource, **Eclipse Public License
    v1.0**) and **Apache Commons IO** (**Apache-2.0**); their notices are
    preserved inside those directories.

## Upstream

NewTube is an independent, unofficial fork of **SmartTube** (© yuliskov, MIT —
https://github.com/yuliskov/SmartTube). Not affiliated with or endorsed by
SmartTube's developer, Google, or YouTube. "YouTube" and "Android" are
trademarks of Google LLC.
