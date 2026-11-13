---
status: accepted
date: 2026-11-12
decision-makers: Oliver Kopp
---

# Colors Come from the Palettes Nord and Everforest

`adr~colors-from-published-palettes~1`

Needs: dsn

## Context and Problem Statement

The application used the baseline color scheme of Material 3.
The colors should be those of a palette the user knows from the editor and the terminal: Nord, and Everforest as an alternative, each in a light and a dark variant.
A Material color scheme has about thirty roles, among them container colors for labels; the palettes have a dozen colors and no containers.
How do the colors of a palette become a color scheme?

## Considered Options

* The colors of the palette fill the roles directly; containers are mixed from them
* A scheme generated from one color of the palette
* The baseline scheme of Material 3

## Decision Outcome

Chosen option: "The colors of the palette fill the roles directly; containers are mixed from them", because the application then shows the colors the palette defines.

The setting *Colors* chooses the palette, Nord by default, and whether it is light, dark or as the system.
Backgrounds, text and accents are colors of the palette.
A container, such as the background of a label, is the color of its role mixed into the color of the cards.
The text on an accent color is the lighter or the darker end of the palette, whichever has the higher contrast.
Everforest is used with its medium contrast.

### Consequences

* Good, because the application fits an environment that uses the same palette.
* Good, because a further palette is one more set of a dozen colors.
* Bad, because the contrast is that of the palette: text in the light variant of Everforest stays below the ratio of 4.5 that is recommended for body text.
* Bad, because the mixed container colors are not colors of the palette.

## Pros and Cons of the Options

### A scheme generated from one color of the palette

The Material tools derive tonal palettes for all roles from a seed color.

* Good, because the contrast between the roles is guaranteed.
* Bad, because only the hue of the seed remains; the result does not contain the colors of Nord or Everforest.

### The baseline scheme of Material 3

* Good, because there is nothing to maintain.
* Bad, because there is no choice, and the colors differ from those of the tools next to the application.
