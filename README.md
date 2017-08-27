Introduction
====

FlexibleTabLayout is a modified Android TabLayout that surfaces more content that can be modified.
As of now user's can only change font and SlidingTabStrip width. Any requests will be taken into
consideration and added to to library as needed.

Change SlidingTabStrip width

```
tabLayout.setStripWidth(40);
```

Have SlidingTabStrip width match the text width

```
tabLayout.setMatchTextWidth(true);
```

Change Text Font

```
tabLayout.setTypeface((TypeFace) waltTypeFace);
```

![Demo](https://cloud.githubusercontent.com/assets/4589397/25103335/1d1df160-238a-11e7-809f-bc9f3ce9b871.gif)

Download
--------

Gradle
```groovy
compile 'com.tiensinoakuma.flexibletablayout:flexibletablayout:0.2.7'
```

License
=======

    Copyright 2017 Tiensi

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
