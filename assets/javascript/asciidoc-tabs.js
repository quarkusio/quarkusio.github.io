// code originally coming from:
// https://github.com/bmuschko/asciidocj-tabbed-code-extension
// adapted to work with jQuery

$(document).ready(function() {
    function addBlockSwitches() {
        $('.listingblock.primary, .sidebarblock.primary').each(function() {
            var primary = $(this);
            createSwitchItem(primary, createBlockSwitch(primary)).item.addClass("selected");
            if (primary.children('.title').length) {
                primary.children('.title').remove();
            } else {
                primary.children('.content').first().children('.title').remove();
            }
        });

        $('.listingblock.secondary, .sidebarblock.secondary').each(function(idx, node) {
            var secondary = $(node);
            var primary = findPrimary(secondary);
            var switchItem = createSwitchItem(secondary, primary.children('.asciidoc-tabs-switch'));
            switchItem.content.addClass('asciidoc-tabs-hidden');
            findPrimary(secondary).append(switchItem.content);
            secondary.remove();
        });
    }

    function createBlockSwitch(primary) {
        var blockSwitch = $('<div class="asciidoc-tabs-switch"></div>');
        primary.prepend(blockSwitch);
        return blockSwitch;
    }

    function findPrimary(secondary) {
        return secondary.prev('.primary');
    }

    function createSwitchItem(block, blockSwitch) {
        var blockName;
        if (block.children('.title').length) {
            blockName = block.children('.title').text();
        } else {
            blockName = block.children('.content').first().children('.title').text();
            block.children('.content').first().children('.title').remove();
        }
        var content = block.children('.content').first().append(block.next('.colist'));
        var item = $('<div class="asciidoc-tabs-switch--item">' + blockName + '</div>');
        item.on('click', '', content, function(e) {
            $(this).addClass('selected');
            $(this).siblings().removeClass('selected');
            e.data.siblings('.content').addClass('asciidoc-tabs-hidden');
            e.data.removeClass('asciidoc-tabs-hidden');
        });
        blockSwitch.append(item);
        return {'item': item, 'content': content};
    }

    addBlockSwitches();
});
