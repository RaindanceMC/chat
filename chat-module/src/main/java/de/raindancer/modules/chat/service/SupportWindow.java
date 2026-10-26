package de.raindancer.modules.chat.service;

import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/** {@link SupportLetter} as one of Minecraft's dialog windows, the same kind the help guide uses. No logic of its own. */
public final class SupportWindow {

    private static final int TEXT_WIDTH = 260;
    private static final int BUTTON = 140;

    private SupportWindow() {
    }

    public static void show(Player player, SupportLetter letter, String link, boolean wearingPack) {
        List<DialogBody> body = new ArrayList<>();
        letter.header().ifPresent(header -> body.add(DialogBody.plainMessage(header, TEXT_WIDTH)));
        body.add(DialogBody.item(SupportPack.mug(wearingPack)).showTooltip(false)
                .description(DialogBody.plainMessage(letter.greeting(), TEXT_WIDTH - 40)).build());
        body.add(DialogBody.plainMessage(letter.text(), TEXT_WIDTH));
        letter.signature().ifPresent(signature -> body.add(DialogBody.plainMessage(signature, TEXT_WIDTH)));
        letter.reward().ifPresent(reward -> body.add(DialogBody.plainMessage(reward, TEXT_WIDTH)));
        body.add(DialogBody.plainMessage(letter.footer(), TEXT_WIDTH));

        ActionButton buy = ActionButton.builder(letter.buy()).tooltip(letter.buyTooltip()).width(BUTTON)
                .action(DialogAction.staticAction(ClickEvent.openUrl(link))).build();
        ActionButton later = ActionButton.builder(letter.later()).width(BUTTON).build();
        List<ActionButton> actions = new ArrayList<>(List.of(buy));
        if (letter.copy().isPresent() && letter.code().isPresent()) {
            // Every button closes the window: with the client's "none" its footer button would not close it either.
            actions.add(ActionButton.builder(letter.copy().get()).tooltip(letter.copyTooltip()).width(BUTTON)
                    .action(DialogAction.staticAction(ClickEvent.copyToClipboard(letter.code().get()))).build());
        }
        player.showDialog(Dialog.create(factory -> factory.empty()
                .base(DialogBase.builder(letter.title()).canCloseWithEscape(true).pause(false)
                        .afterAction(DialogBase.DialogAfterAction.CLOSE).body(body).build())
                .type(DialogType.multiAction(actions).exitAction(later).columns(1).build())));
    }
}
