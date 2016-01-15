package de.codeshelf.consoleui.prompt;

import de.codeshelf.consoleui.elements.ExpandableChoice;
import de.codeshelf.consoleui.elements.items.ConsoleUIItemIF;
import de.codeshelf.consoleui.elements.items.impl.ChoiceItem;
import de.codeshelf.consoleui.prompt.reader.ConsoleReaderImpl;
import de.codeshelf.consoleui.prompt.reader.ReaderIF;
import de.codeshelf.consoleui.prompt.renderer.CUIRenderer;
import org.fusesource.jansi.Ansi;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;

import static org.fusesource.jansi.Ansi.ansi;

/**
 * User: Andreas Wegmann
 * Date: 07.01.16
 */
public class ExpandableChoicePrompt extends AbstractPrompt implements PromptIF<ExpandableChoice> {
  private ConsoleReaderImpl reader;
  private ExpandableChoice expandableChoice;
  CUIRenderer itemRenderer = CUIRenderer.getRenderer();
  private int selectedItemIndex;
  ChoiceItem choosenItem;
  ChoiceItem defaultItem;
  private ChoiceItem errorMessageItem = new ChoiceItem(' ', "error", resourceBundle.getString("please.enter.a.valid.command"));
  ArrayList<ConsoleUIItemIF> itemList;

  enum RenderState {
    FOLDED,
    FOLDED_ANSWERED,
    EXPANDED
  }

  RenderState renderState = RenderState.FOLDED;
  LinkedHashSet<ChoiceItem> choiceItems;
  String promptString;

  private void render() {
    if (renderState == RenderState.EXPANDED) {
      renderList();
    } else if (renderState == RenderState.FOLDED) {
      System.out.println("");
      System.out.println(ansi().eraseLine().cursorUp(2));
      System.out.print(renderMessagePrompt(expandableChoice.getMessage()) + " (" + promptString + ") ");
      System.out.flush();
      renderHeight = 1;
    } else if (renderState == RenderState.FOLDED_ANSWERED) {
      System.out.println("");
      System.out.println(ansi().fg(Ansi.Color.CYAN).a(">> ").reset().a(choosenItem.getMessage()).eraseLine());
      System.out.print(ansi().cursorUp(2));
      System.out.print(renderMessagePrompt(expandableChoice.getMessage()) + " (" + promptString + ") ");
      System.out.flush();
      renderHeight = 1;
    }
  }

  private void renderList() {
    if (renderHeight == 1) {
      // first time we expand the list...
      renderHeight = 1 + itemList.size();
      System.out.println("");
      System.out.println(ansi().eraseLine().cursorUp(2).a(renderMessagePrompt(expandableChoice.getMessage())).eraseLine(Ansi.Erase.FORWARD));
      System.out.flush();
    } else {
      System.out.println(ansi().cursorUp(renderHeight));
    }

    int itemNumber = 0;
    for (ConsoleUIItemIF choiceItem : itemList) {
      String renderedItem = itemRenderer.render(choiceItem, (selectedItemIndex == itemNumber));
      System.out.println(renderedItem + ansi().eraseLine(Ansi.Erase.FORWARD));
      itemNumber++;
    }
  }

  public LinkedHashSet<String> prompt(ExpandableChoice expandableChoice) throws IOException {
    this.expandableChoice = expandableChoice;
    if (reader == null) {
      reader = new ConsoleReaderImpl();
    }

    choiceItems = expandableChoice.getChoiceItems();
    promptString = "";

    for (ChoiceItem choiceItem : choiceItems) {
      if (choiceItem.getKey() == 'h') {
        throw new IllegalStateException("expandableChoice may not use the reserved key 'h' for an element.");
      }
      if (defaultItem == null) {
        defaultItem = choiceItem;
      }
      reader.addAllowedPrintableKey(choiceItem.getKey());
      promptString += choiceItem.getKey();
    }

    choiceItems.add(new ChoiceItem('h', resourceBundle.getString("help"), resourceBundle.getString("help.list.all.options")));
    reader.addAllowedPrintableKey('h');
    promptString += "h";
    System.out.println("promptString = " + promptString);
    reader.addAllowedSpecialKey(ReaderIF.SpecialKey.ENTER);
    reader.addAllowedSpecialKey(ReaderIF.SpecialKey.BACKSPACE);
    renderState = RenderState.FOLDED;

    // first render call, we don't need to position the cursor up
    renderHeight = 1;
    render();

    ReaderIF.ReaderInput readerInput = this.reader.read();
    while (true) {
      if (readerInput.getSpecialKey() == ReaderIF.SpecialKey.ENTER) {
        // if ENTER pressed
        if (choosenItem != null && choosenItem.getKey() == 'h') {
          renderState = RenderState.EXPANDED;

          itemList = new ArrayList<ConsoleUIItemIF>();
          itemList.addAll(expandableChoice.getChoiceItems());

          selectedItemIndex = getFirstSelectableItemIndex();
          render();
          reader.addAllowedSpecialKey(ReaderIF.SpecialKey.UP);
          reader.addAllowedSpecialKey(ReaderIF.SpecialKey.DOWN);

          readerInput = this.reader.read();
        } else {
          LinkedHashSet<String> hashSet = new LinkedHashSet<String>();
          if (renderState != RenderState.EXPANDED) {
            System.out.println("");
          } else {
            renderHeight++;
          }
          if (choosenItem != null) {
            renderMessagePromptAndResult(expandableChoice.getMessage(), choosenItem.getMessage());
            hashSet.add(choosenItem.getName());
          } else {
            renderMessagePromptAndResult(expandableChoice.getMessage(), defaultItem.getMessage());
            hashSet.add(defaultItem.getName());
          }
          return hashSet;
        }
      } else if (readerInput.getSpecialKey() == ReaderIF.SpecialKey.UP) {
        this.selectedItemIndex = getPreviousSelectableItemIndex();
      } else if (readerInput.getSpecialKey() == ReaderIF.SpecialKey.DOWN) {
        this.selectedItemIndex = getNextSelectableItemIndex();
      }
      if (readerInput.getSpecialKey() == ReaderIF.SpecialKey.PRINTABLE_KEY) {
        Character pressedKey = readerInput.getPrintableKey();
        if (promptString.toLowerCase().contains("" + pressedKey)) {
          // find the new choosen item
          selectedItemIndex = 0;
          for (ChoiceItem choiceItem : choiceItems) {
            if (choiceItem.getKey() == pressedKey) {
              choosenItem = choiceItem;
              break;
            }
            selectedItemIndex++;
          }
          if (renderState == RenderState.FOLDED) {
            renderState = RenderState.FOLDED_ANSWERED;
          }
        } else {
          // not in valid choices
          choosenItem = errorMessageItem;
        }
      }
      render();
      readerInput = this.reader.read();
    }
  }

  private int getNextSelectableItemIndex() {
    for (int i = 0; i < itemList.size(); i++) {
      int newIndex = (selectedItemIndex + 1 + i) % itemList.size();
      ConsoleUIItemIF item = itemList.get(newIndex);
      if (item.isSelectable())
        return newIndex;
    }
    return selectedItemIndex;
  }

  private int getPreviousSelectableItemIndex() {
    for (int i = 0; i < itemList.size(); i++) {
      int newIndex = (selectedItemIndex - 1 - i + itemList.size()) % itemList.size();
      ConsoleUIItemIF item = itemList.get(newIndex);
      if (item.isSelectable())
        return newIndex;
    }
    return selectedItemIndex;
  }

  private int getFirstSelectableItemIndex() {
    int index = 0;
    for (ConsoleUIItemIF item : itemList) {
      if (item.isSelectable())
        return index;
      index++;
    }
    throw new IllegalStateException("no selectable item in list");
  }


}