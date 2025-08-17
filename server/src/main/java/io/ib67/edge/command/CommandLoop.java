/*
 *    Copyright 2025 iceBear67 and Contributors
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 */

package io.ib67.edge.command;

import io.ib67.kiwi.ArgOpts;
import lombok.extern.log4j.Log4j2;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Map;
import java.util.Scanner;
import java.util.function.Function;
import java.util.stream.Collectors;

@Log4j2
public class CommandLoop {
    protected final Map<String, Command> commands;

    public CommandLoop(Command... commands) {
        this.commands = Arrays.stream(commands)
                .collect(Collectors.toMap(Command::name, Function.identity()));
    }

    public void run(InputStream in) {
        var scanner = new Scanner(in);
        scanner.useDelimiter("\n");
        var command = new StringBuilder();
        while (scanner.hasNextLine()) {
            var line = scanner.nextLine();
            if (line.endsWith("\\") && line.length() > 1) {
                command.append(line, 0, line.length() - 2);
                continue;
            } else {
                command.append(line.trim());
            }

            var prompt = command.toString().trim();
            command = new StringBuilder();
            var firstSpace = prompt.indexOf(' ');
            var programName = firstSpace < 0 ? prompt : prompt.substring(0, firstSpace);
            var cmd = commands.get(programName);
            if (cmd != null) {
                var promptSplit = prompt.split(" ");
                try {
                    cmd.execute(System.out, ArgOpts.builder()
                            .args(promptSplit)
                            .programName(programName)
                            .description(cmd.description())
                            .build()
                    );
                } catch (Exception ex) {
                    log.error("An error occurred when executing prompt {}", prompt, ex);
                }
                continue;
            }
            if(prompt.equalsIgnoreCase("help")){
                log.info("Available commands:");
                commands.values().stream().map(
                        it->"  - " + it.name()+" "+it.description()
                ).forEach(log::info);
                continue;
            }
            log.error("Invalid command.");
        }
    }
}
