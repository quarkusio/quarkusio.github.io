function addBlockSwitches() {
    $('.listingblock.primary, .sidebarblock.primary').each(function() {
        primary = $(this);
        createSwitchItem(primary, createBlockSwitch(primary)).item.addClass("selected");
        if (primary.children('.title').length) {
            primary.children('.title').remove();
        } else {
            primary.children('.content').first().children('.title').remove();
        }
    });
    $('.listingblock.secondary, .sidebarblock.secondary').each(function(idx, node) {
        secondary = $(node);
        primary = findPrimary(secondary);
        switchItem = createSwitchItem(secondary, primary.children('.switch'));
        switchItem.content.addClass('hidden');
        findPrimary(secondary).append(switchItem.content);
        secondary.remove();
    });
}

function createBlockSwitch(primary) {
    blockSwitch = $('<div class="switch"></div>');
    primary.prepend(blockSwitch);
    return blockSwitch;
}

function findPrimary(secondary) {
    return secondary.prev('.primary');
}

function createSwitchItem(block, blockSwitch) {
    if (block.children('.title').length) {
        blockName = block.children('.title').text();
    } else {
        blockName = block.children('.content').first().children('.title').text();
        block.children('.content').first().children('.title').remove();
    }
    content = block.children('.content').first().append(block.next('.colist'));
    item = $('<div class="switch--item">' + blockName + '</div>');
    item.on('click', '', content, function(e) {
        $(this).addClass('selected');
        $(this).siblings().removeClass('selected');
        e.data.siblings('.content').addClass('hidden');
        e.data.removeClass('hidden');
    });
    blockSwitch.append(item);
    return {'item': item, 'content': content};
}

$(document).ready(function() {
    addBlockSwitches();
});
