# 🌟 MiniGameHub Plugin 🌟

Welcome to **MiniGameHub**, the ultimate Minecraft plugin that transforms your server into a thrilling arena of excitement and adventure! With MiniGameHub, server administrators can effortlessly host a variety of exhilarating mini-games, including the intense **Survival Games**, the fast-paced **Spleef**, and the mind-bending **DeathSwap**. 

Seamlessly integrating with Multiverse-Core, MiniGameHub ensures smooth management of multiple worlds, delivering an unparalleled and immersive experience for all players. Get ready to elevate your Minecraft server to new heights of fun and competition!

## Features

- **Survival Games**: A battle royale-style game where players fight to be the last one standing.
- **Spleef**: A game where players try to break blocks under their opponents to make them fall.
- **DeathSwap**: A game where players swap positions at random intervals, trying to set traps for each other.

## Installation

1. Download the MiniGameHub plugin jar file.
2. Place the jar file in your server's `plugins` directory.
3. Ensure that you have Multiverse-Core installed on your server.
4. Start or restart your server to load the plugin.

## Configuration

The plugin comes with default configuration files for each game mode. These files are located in the `plugins/MiniGameHub` directory. You can customize the settings by editing these files.

### Config Files

- `config.yml`: Main configuration file.
- `survivalgames.yml`: Configuration for Survival Games.
- `deathswap.yml`: Configuration for DeathSwap.
- `spleef.yml`: Configuration for Spleef.

## Commands

### General Commands

- `/minigame start <game> <world> [player1] [player2] ...`: Starts the specified game in the given world with the listed players.
- `/minigame setup <game> <world>`: Enters setup mode for the specified game in the given world.

### Survival Games Commands

- `/minigame start survivalgames <world> <player1> <player2> ...`: Starts a Survival Games match in the specified world with the listed players.
- `/minigame setup survivalgames <world>`: Enters setup mode for Survival Games in the specified world.

### Spleef Commands

- `/minigame start spleef <world> <player1> <player2> ...`: Starts a Spleef match in the specified world with the listed players.

### DeathSwap Commands

- `/minigame start deathswap <player1> <player2>`: Starts a DeathSwap match with the listed players.

## Usage

### Starting a Game

To start a game, use the `/minigame start` command followed by the game name, world name, and player names. For example:

### Survival Games Creator Mode

The Survival Games creator mode allows server administrators to set up spawn points for players in a specific world. This mode is essential for preparing the game environment before starting a match. To enter the creator mode, use the command `/minigame setup survivalgames <world>`.

Once in creator mode, you can set spawn points by right-clicking blocks with a stick. Each right-click will register the location as a spawn point for the specified world. The plugin will store these locations and use them to teleport players when the game starts.

To exit the creator mode, simply stop interacting with the blocks or use any other command. The spawn points will be saved and can be used in future matches. This setup ensures that all players have designated starting positions, making the game fair and organized.

## Dependencies

MiniGameHub relies on the following dependencies to function correctly:

- **Multiverse-Core**: For managing multiple worlds.
- **Spigot API**: For interacting with the Minecraft server.

Ensure these dependencies are installed and properly configured on your server to enjoy the full functionality of MiniGameHub.

## Support

If you encounter any issues or have questions about MiniGameHub, please visit our [support page](https://example.com/support) or join our [Discord server](https://example.com/discord) for assistance.

## Contributing

We welcome contributions from the community! If you would like to contribute to MiniGameHub, please fork the repository and submit a pull request. For major changes, please open an issue first to discuss what you would like to change.

Thank you for using MiniGameHub! We hope you and your players have an amazing time with our plugin.
