function addBlockSwitches() {
    $('.listingblock.primary').each(function() {
        primary = $(this);
        createSwitchItem(primary, createBlockSwitch(primary)).item.addClass("selected");
        primary.children('.title').remove();
    });
    $('.listingblock.secondary').each(function(idx, node) {
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
    blockName = block.children('.title').text();
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
