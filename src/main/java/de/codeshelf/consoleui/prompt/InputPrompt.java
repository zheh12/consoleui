package de.codeshelf.consoleui.prompt;

import de.codeshelf.consoleui.elements.InputValue;
import de.codeshelf.consoleui.prompt.reader.ConsoleReaderImpl;
import de.codeshelf.consoleui.prompt.reader.ReaderIF;
import de.codeshelf.consoleui.prompt.renderer.CUIRenderer;
import jline.console.completer.Completer;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.List;

import static org.fusesource.jansi.Ansi.ansi;

/**
 * User: Andreas Wegmann
 * Date: 06.01.16
 */
public class InputPrompt extends AbstractPrompt implements PromptIF<InputValue> {

  private InputValue inputElement;
  private ReaderIF reader;
  CUIRenderer itemRenderer = CUIRenderer.getRenderer();


  public LinkedHashSet<String> prompt(InputValue inputElement) throws IOException {
    this.inputElement = inputElement;

    if (reader == null) {
      reader = new ConsoleReaderImpl();
    }

    if (renderHeight == 0) {
      renderHeight = 1;
    } else {
      System.out.println(ansi().cursorUp(renderHeight));
    }

    String prompt = renderMessagePrompt(this.inputElement.getMessage()) + itemRenderer.renderOptionalDefaultValue(this.inputElement);
    //System.out.print(prompt + itemRenderer.renderValue(this.inputElement));
    //System.out.flush();
    List<Completer> completer = inputElement.getCompleter();
    ReaderIF.ReaderInput readerInput = reader.readLine(completer,prompt,inputElement.getValue());

    String lineInput = readerInput.getLineInput();

    if (lineInput == null || lineInput.trim().length() == 0) {
      lineInput = inputElement.getDefaultValue();
    }
    renderMessagePromptAndResult(inputElement.getMessage(), lineInput);

    LinkedHashSet<String> resultSet = new LinkedHashSet<String>();
    resultSet.add(lineInput);
    return resultSet;
  }
}
