# Rythme product and motion baseline

This is the product contract for architecture and motion work. It records behavior that must survive refactors and separates
intentional interaction complexity from historical implementation complexity.

## Product scope

Rythme is an Android local music player inspired by the current Apple Music interaction and visual language. Reliable local
catalog, playback, queue, playlists, search, metadata overrides, and lyrics take priority over placeholder online-service UI.
Online lyrics are optional; the app does not require an account.

## Architecture rules

- Use the smallest feature structure that fits the behavior. A page does not require a Contract or ViewModel by default.
- Navigation belongs to the route/UI boundary. ViewModels expose data and operations, not destinations.
- App Chrome, content navigation, Player, and Menu are separate layers.
- Player is an overlay, not a route, so the scaffold and per-tab stacks remain alive.
- Significant shared motion is owned by the layer that provides visual continuity, not by business ViewModels.
- Existing placeholder UI records planned product intent. Preserve it and implement its behavior; do not remove or hide it
  without an explicit product decision.
- New visible controls must have implemented behavior.

## Navigation invariants

- Home, Playlist, Library, and Search have independent back stacks and preserve page state.
- Selecting a top-level tab does not use a detail-page push transition.
- Push, pop, and tab selection are distinct content navigation operations.
- Content navigation never disables TopBar or BottomBar motion globally.
- Back priority is Menu/Editor, Player internal panel, Player overlay, current tab stack, start tab, then app exit.

## BottomBar

- Expanded: all primary tabs and Search are inside one outer capsule; Search is a visually grouped ordinary tab.
- Expanded: MiniPlayer is an attached accessory immediately above the tab capsule.
- Collapsed: show the most recently selected non-Search tab, MiniPlayer, and Search in one row.
- `lastPrimaryTab` changes only when a non-Search tab is selected.
- Tapping any tab forces expansion, including tapping the selected tab.
- Collapse/expansion is triggered by accumulated downward/upward user scroll past a threshold.
- Presentation is event-driven and is not restored from a destination's absolute scroll position.
- Changing scroll direction resets the accumulated threshold distance.
- BottomBar remains fixed to the window and is covered by the IME.
- Search query, focus, and results belong to SearchScreen.

## TopBar and menus

- The global TopBar remains composed across page transitions.
- Back is an independent circular surface; trailing actions form a stable action-group capsule.
- Action identity is defined by stable visual IDs. Callback identity must not drive motion.
- TopBar menus expand from the complete trailing action-group bounds.
- Song context menus expand from the tapped song More control and choose direction from available space.
- Menus use a transparent outside-touch interceptor; do not add an unobserved dim or blur effect.
- Fixed delays must not own transition lifetime.

## Player

- MiniPlayer to Player is an overlay container transform, not a normal page fade or push.
- Container, artwork, song information, background, and controls form one coordinated transition.
- Player remembers its last internal panel while it remains in app state.
- Now Playing is expanded; Lyrics and Queue are compact presentations sharing one compact header.
- Now Playing to compact mode shares artwork and song information.
- Lyrics to Queue keeps the compact header in place and replaces only the body.
- Back from Lyrics/Queue returns to Now Playing; Back from Now Playing closes Player.

## Queue and History

- Queue and History are independent vertically scrollable surfaces with independent positions.
- History is above Queue and has a sticky title and Clear action.
- Queue has a collapsible current-song header, Playing Next, and Autoplay content.
- A surface consumes gestures while it can scroll. Only unconsumed boundary motion advances the surface switch.
- Queue upward motion collapses the current-song header before scrolling its list.
- Queue downward motion returns its list to the top and expands the header before switching to History.
- Queue item reordering temporarily prevents surface switching or Player dismissal.
- The surface switch uses vertical translation only. Do not add scale, stretch, blur pulses, alpha crossfades, or corner morphs
  without direct Apple Music reference evidence.

## Shared-element identity

- Catalog items use stable catalog identity (`trackKey`, `albumKey`).
- Queue and Player items use queue-entry identity so explicit duplicate tracks remain distinguishable.
- Surfaces use shared bounds/container transforms; artwork uses shared content.
- Content and overlay shared-transition scopes are separate by visual layer, not incidental feature names.

## Verification

- UI and motion changes require Android-device verification; compilation alone is insufficient.
- Compare core motion against iPhone reference recordings at slow drag, reverse drag, short release, threshold release, and fling.
- Verify interruptions around 20%, 50%, and 80% progress.
- Verify rapid navigation, animation reversal, song changes during transition, IME, large fonts, and TalkBack alternatives.
