# AI Report

**Runway, PROG7314 POE Part 2, Group 11**

Generative AI was used in a limited supporting role on this project. It did not design or write the application: every screen, endpoint and test was specified and implemented by the team. The uses below are the complete list.

## Branding

Early logo concepts were explored with a generative image tool to settle on a direction: letterforms, weight, and how a mark would hold up at icon size. None of that output appears in the product. The final Runway logo was drawn from scratch in **Adobe Illustrator** and finished in **Adobe Photoshop**, and the asset shipped in the repository is that artwork.

## Structuring comments and documentation

Four developers worked in parallel, and comment style drifted between them. AI was used to bring the codebase to one standard: rewriting comments into plain English, cutting them back to a single line where that was enough, and checking that a comment explained *why* a piece of code exists rather than restating what the next line already says. The same assistance was used to format the Markdown documentation in this repository, including the structure of this README.

## Debugging

Where a failure was not obvious from its message, AI was used as a second reader of the log or stack trace to narrow down where to look. Two examples:

- a `FAILED_PRECONDITION` returned by a Firestore query, which turned out to need a composite index rather than a code change
- Gradle builds failing on file locks rather than on anything in the source

In both cases the diagnosis was confirmed against the running system before anything was changed.

## Cleaning up code

After the team replaced a planned feature with a different one, AI was used to help track down what the removed feature had left behind: unused imports, strings, resources and dependencies that nothing referenced any more. It was also used to review our own code for repetition worth extracting into a shared helper. Suggested changes were read, compiled and put through the full test suite before any of them were kept.

## Limits the team set

- No feature was generated wholesale and accepted unread. Anything suggested was reviewed line by line by the developer responsible for that area.
- **No external AI service is called by the app or the API at runtime.** The only model in the running product is ML Kit subject segmentation, which removes the background from a garment photo on the device itself and is a standard Android platform capability.
- Every AI-assisted change had to compile and pass the existing 210 unit tests before it was committed.
