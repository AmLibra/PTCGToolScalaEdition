# PTCGToolScalaEdition
    A port of my initial PTCGTool Program in Scala. This is a work in progress and is not yet complete.

## What is this
    This is a program that allows you to search, view and add Pokémon cards to your local collection of decks.
    These decks are stored in a local database and will be saved and easily exported to a file.
    You can similarly import decks from a file and add them to your collection.

    The second big feature of this program is the ability to compute all sorts of statistics about your decks.  
    You can also simulate opening hand draws and see how likely it is that you will draw a certain card.
 
    Another feature would be to fetch win rates for deck archetypes from the most recent tournaments. 
    This would be useful to compute the win rate of your deck archetype against other decks, and thus select the 
    best archetype for your deck.

    This tool is designed to be efficient and easy to use. 

## How to use

    // TODO

## How does it work
    The program is fully written in Scala and uses the ScalaFX library for the GUI.
    It uses a local database to store all the decks and cards, which is stored in the `data` folder,
    and the [Pokémon TCG API](https://docs.pokemontcg.io/) to fetch all the cards and their data.
    
    The program initially fetches all the cards from the API and stores them in the database, if not already present.
    It then generates a more optimized search index for the cards, which is stored in the `data_opt` folder.
    This index is used to quickly search for cards and is updated whenever a new card is added to the database.

## Structure 
    package ptcgtool contains the full program. 

    package ptcgtool.frontend contains the GUI and all the components only depending on ScalaFX.
        This inludes the application tab windows: DeckBuilder and Statistics.

    package ptcgtool.backend contains all the logic and data structures of the program, this is the core of the program.
        This includes all the objects representing a card, a deck, I/O tools and utility functions to compute statistics.

    package ptcgtool.api contains the API functions to fetch the cards from the API and store them in the database.
        This includes the CardFetcher which deals with the API Calls. 
        This isolates the API calls from the rest of the program.
