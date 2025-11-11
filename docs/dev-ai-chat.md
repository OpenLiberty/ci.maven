### dev-ai-chat

-----

#### For development:

Do the following to build the ci.common `99.0.0-SNAPSHOT` 
```
git clone git@github.com:OpenLiberty/ci.common.git -b dev-ai-chat
cd ci.common
./mvnw clean install
```
and do the following to build the ci.maven `99.0.0-SNAPSHOT` 
```
git clone git@github.com:OpenLiberty/ci.maven.git -b dev-ai-chat
cd ci.maven
./mvnw clean install
```


#### Prerequisites

- Download and install [Ollama](https://ollama.com/download).
  -   see the [README.md](https://github.com/ollama/ollama/blob/main/README.md#ollama).
- Pull the following model.
  -   `ollama pull gpt-oss`
- Update the Liberty Maven Plugin version to `99.0.0-SNAPSHOT` in your Maven `pom.xml` project file.

#### Usages

After you start dev mode, you will see
```
[INFO] ************************************************************************
[INFO] *    Liberty is running in dev mode.
[INFO] *        Automatic generation of features: [ Off ]
[INFO] *        a - toggle the AI mode, type 'a' and press Enter.
[INFO] *        h - see the help menu for available actions, type 'h' and press Enter.
[INFO] *        q - stop the server and quit dev mode, press Ctrl-C or type 'q' and press Enter.
[INFO] *    Liberty server port information:
[INFO] *        Liberty server HTTP port: [ 9080 ]
[INFO] *        Liberty server HTTPS port: [ 9443 ]
[INFO] *        Liberty debug port: [ 7777 ]
[INFO] ************************************************************************
```

Press `a` and `Enter` keys to toggle the AI mode on or off.

When you turn it on, by default, the Ollama base URL `http://localhost:11434` and the `gpt-oss` model will be connected.

When the connection is established, you will see the following help:
```
[INFO] ************************************************************************
[INFO] *        
[INFO] *    AI information:
[INFO] *        model: gpt-oss
[INFO] *        
[INFO] *        To start a multi-line message, type in [ and press Enter.
[INFO] *            To end the multi-line message, type in ] and press Enter.
[INFO] *            To clear the multi-line message, press Ctrl+X followed by Ctrl+K.
[INFO] *        Reset chat session - type in reset chat and press Enter.
[INFO] *        View a previous message - press Up/Down arrow key.
[INFO] *        
[INFO] ************************************************************************
```

When you type in any message that is not any hotkey such as `a`, `g`, `h`, `q`, or `r` ...etc., the message will be chatted with the Ollama model. For example:
```
What is LLM?
                                        
┌───────────────────────────────────────────────────────────────────────────────

LLM stands for Large Language Model—a type of artificial‑intelligence model trained on vast amounts of text to generate, interpret, or manipulate natural language.

Source: Wikipedia – Large language model (https://en.wikipedia.org/wiki/Large\_language\_model)

└───────────────────────────────────────────────────────────────────────────────
```

#### Override the Ollama setting

To override the Ollama base URL, start the dev mode with the `ollama.base.url` system property:
```
./mvnw -Dollama.base.url=<an Ollama URL> liberty:dev
```

To override the Ollama model, start the dev mode with the `chat.model.id` system property:
```
./mvnw -Dchat.model.id=devstral:latest liberty:dev
```
